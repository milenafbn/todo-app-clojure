(ns todo.frontend.core
  (:require [reagent.core :as r]
            [reagent.dom.client :as rdom]
            [clojure.string :as str]))

;; --- O "Cérebro" Reativo ---
(defonce app-state (r/atom {:next-id 1
                            :input-text ""
                            :todos []}))

;; --- A Lógica de Negócios (Local) ---
(defn adicionar-todo-local []
  (swap! app-state
         (fn [estado-atual]
           (let [novo-titulo (:input-text estado-atual)
                 novo-id (:next-id estado-atual)]
             (if (str/blank? novo-titulo)
               estado-atual ;; Não faz nada se vazio
               ;; Retorna um NOVO estado
               {:next-id (inc novo-id)
                :input-text "" ;; Limpa o input
                :todos (conj (:todos estado-atual)
                             {:id novo-id
                              :title novo-titulo})})))))

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
    ;; (Ação): o on-click chama nossa lógica local
    {:on-click adicionar-todo-local}
    "Adicionar (Local)"]])

;; A Lista (recebe uma lista de "todos" como argumento)
(defn todo-list [] ;; <-- Argumento "todos" REMOVIDO
  [:ul.todo-list
   ;; (Leitura): o 'for' agora observa o @app-state
   (for [todo (:todos @app-state)]
     ^{:key (:id todo)}
     [:li.todo-item
      (:title todo)])])

;; O App Principal (que monta tudo)
(defn app []
  [:div.todo-app
   [:h1 "Todo App (Somente Frontend)"]
   [:p "Isto é 100% local. Recarregue (F5) para ver os dados sumirem."]

   ;; Os componentes agora se viram sozinhos!
   [todo-form]
   [todo-list]])

;; --- 2. A Inicialização ---
(defn ^:export init []
  (println "Frontend 'Todo Estático' inicializado...")
  (let [root (rdom/create-root (js/document.getElementById "app"))]
    (.render root (r/as-element [app]))))