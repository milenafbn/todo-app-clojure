(ns todo.frontend.core
  (:require [reagent.core :as r]
            [reagent.dom.client :as rdom]
            [clojure.string :as str]
            [cljs.core.async :refer [go]]
            [cljs.core.async.interop :refer-macros [<p!]]))

;; --- O "Cérebro" Reativo ---
(defonce app-state (r/atom {:input-text ""
                            :todos []
                            ;; Filtro da lista: :todas, :ativas ou :concluidas
                            :filter :todas}))

;; --- A URL base da nossa API (o backend, porta 3000) ---
(def api-url "http://localhost:3000/api")

;; Função auxiliar: faz o fetch e converte a resposta JSON
;; em dados Clojure (mapas com keywords).
(defn fetch-json [url options]
  (-> (js/fetch url (clj->js options))
      (.then (fn [response]
               (when-not (.-ok response)
                 (throw (js/Error. (str "HTTP error: " (.-status response)))))
               (.json response)))
      ;; Converte o objeto JS em dados Clojure com chaves keyword
      (.then #(js->clj % :keywordize-keys true))))

;; Busca todos os "todos" da API
(defn get-todos []
  (swap! app-state assoc :loading true :error nil)
  (go
    (try
      (let [response (<p! (fetch-json (str api-url "/todos") {:method "GET"}))]
        (swap! app-state assoc :todos (:todos response) :loading false))
      (catch js/Error e
        (swap! app-state assoc :error (.-message e) :loading false)))))

;; --- Cria um "todo" via API (POST) ---
(defn create-todo [todo-data]
  (swap! app-state assoc :loading true :error nil)
  (go
    (try
      (<p! (fetch-json (str api-url "/todos")
                       {:method "POST"
                        :headers {"Content-Type" "application/json"}
                        ;; Converte o mapa Clojure em uma string JSON
                        :body (js/JSON.stringify (clj->js todo-data))}))

      ;; Se o POST funcionou, recarregamos a lista
      (get-todos)
      (catch js/Error e
        (swap! app-state assoc :error (.-message e) :loading false)))))

(defn toggle-todo
  "Chama a API para alternar o status de um todo."
  [id]
  (go
    (try
      (<p! (fetch-json (str api-url "/todos/" id "/toggle")
                       {:method "POST"}))
      ;; Se funcionou, recarrega a lista inteira
      (get-todos)
      (catch js/Error e
        (swap! app-state assoc :error (.-message e) :loading false)))))

(defn delete-todo
  "Chama a API para remover um todo."
  [id]
  (go
    (try
      (<p! (fetch-json (str api-url "/todos/" id)
                       {:method "DELETE"}))
      ;; Se funcionou, recarrega a lista
      (get-todos)
      (catch js/Error e
        (swap! app-state assoc :error (.-message e) :loading false)))))

;; --- 1. Nossos Componentes ---

;; O Formulário
(defn todo-form []
  [:div.todo-input
   [:input
    {:type "text"
     :placeholder "O que precisa ser feito?"
     ;; (Leitura): o valor do input vem do app-state
     :value (:input-text @app-state)
     ;; (Escrita): o on-change atualiza o app-state a cada tecla
     :on-change #(swap! app-state assoc :input-text (-> % .-target .-value))}]

   [:button
    ;; (Ação): o on-click chama a API (POST) e limpa o input
    {:on-click (fn []
                 ;; Não envia título vazio (evita o 400 do backend)
                 (when-not (str/blank? (:input-text @app-state))
                   (create-todo {:title (:input-text @app-state)})
                   (swap! app-state assoc :input-text "")))} ;; Limpa o input
    "Adicionar"]])

;; Um todo está pendente quando o completed (0/1 do SQLite) é 0
(defn pendente? [todo]
  (= 0 (:todos/completed todo)))

;; Aplica o filtro escolhido à lista de todos
(defn filtrar-todos [filtro todos]
  (case filtro
    :ativas     (filter pendente? todos)
    :concluidas (remove pendente? todos)
    todos))

;; A Lista (já filtrada pelo :filter do app-state)
(defn todo-list []
  (let [{:keys [todos error] filtro :filter} @app-state
        visiveis (filtrar-todos filtro todos)]
    (cond
      ;; Com erro na API, a faixa vermelha já explica a lista vazia
      error nil

      ;; Lista vazia: uma mensagem no lugar do <ul> vazio
      (empty? todos)
      [:p.empty-list "Nenhuma tarefa por aqui. Que tal adicionar a primeira?"]

      ;; Há tarefas, mas nenhuma neste filtro
      (empty? visiveis)
      [:p.empty-list "Nenhuma tarefa neste filtro."]

      :else
      [:ul.todo-list
       (for [todo visiveis]
         ^{:key (:todos/id todo)}
         ;; 1. Classe CSS 'completed' quando o status for 1
         [:li.todo-item {:class (when (= 1 (:todos/completed todo)) "completed")}

          ;; 2. O Checkbox
          [:input.todo-checkbox
           {:type "checkbox"
            ;; 3. A CONVERSÃO: 0/1 (SQLite) -> booleano de verdade
            :checked (not= 0 (:todos/completed todo))
            ;; 4. O evento chama nossa função de API
            :on-change #(toggle-todo (:todos/id todo))}]

          ;; 5. O título (dentro de um span, para o CSS)
          [:span.todo-title (:todos/title todo)]

          ;; 6. O botão de deletar
          [:button.delete-btn
           ;; Pede confirmação antes de apagar
           {:on-click #(when (js/confirm "Tem certeza de que quer remover esta tarefa?")
                         (delete-todo (:todos/id todo)))}
           "X"]])])))

;; Os Filtros + o contador de pendentes (rodapé da lista)
(defn todo-filters []
  (let [{:keys [todos error] filtro :filter} @app-state
        pendentes (count (filter pendente? todos))]
    ;; Só aparece quando há tarefas e a API respondeu
    (when (and (not error) (seq todos))
      [:div.todo-footer
       [:span.todo-count
        (str pendentes (if (= 1 pendentes)
                         " tarefa pendente"
                         " tarefas pendentes"))]
       [:div.filters
        (for [[chave rotulo] [[:todas "Todas"]
                              [:ativas "Ativas"]
                              [:concluidas "Concluídas"]]]
          ^{:key chave}
          [:button.filter-btn
           {:class (when (= filtro chave) "active")
            :on-click #(swap! app-state assoc :filter chave)}
           rotulo])]])))

;; O Status da API: mostra o :loading e o :error que o app-state
;; já guarda desde a Fase 4 (extensão opcional do tutorial).
;; Obs.: o <p! embrulha o erro original, então o :error sempre vem
;; como "Promise error" -- por isso exibimos uma mensagem fixa.
(defn api-status []
  (let [{:keys [loading error]} @app-state]
    (cond
      error   [:div.error "Não foi possível conectar ao servidor. Verifique se ele está ligado e recarregue a página."]
      loading [:div.loading "Carregando suas tarefas..."])))

;; O App Principal (que monta tudo)
(defn app []
  [:div.todo-app
   [:h1 "Minhas Tarefas"]
   [:p "Organize seu dia, uma tarefa de cada vez."]

   ;; Os componentes agora se viram sozinhos!
   [todo-form]
   [api-status]
   [todo-list]
   [todo-filters]])

;; --- 2. A Inicialização ---
(defn ^:export init []
  (println "Frontend inicializado...")
  (let [root (rdom/create-root (js/document.getElementById "app"))]
    (.render root (r/as-element [app])))

  ;; Ao carregar a página, busca os todos da API
  (get-todos))