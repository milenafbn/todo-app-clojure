(ns todo.frontend.core
  (:require [reagent.core :as r]
            [reagent.dom.client :as rdom]
            [clojure.string :as str]
            [cljs.core.async :refer [go]]
            [cljs.core.async.interop :refer-macros [<p!]]))

;; --- O "Cérebro" Reativo ---
(defonce app-state (r/atom {:input-text ""
                            :todos []}))

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
                 (create-todo {:title (:input-text @app-state)})
                 (swap! app-state assoc :input-text ""))} ;; Limpa o input
    "Adicionar"]])

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
   [:h1 "Todo App"]
   [:p "Conectado à API. Os dados sobrevivem ao F5!"]

   ;; Os componentes agora se viram sozinhos!
   [todo-form]
   [todo-list]])

;; --- 2. A Inicialização ---
(defn ^:export init []
  (println "Frontend inicializado...")
  (let [root (rdom/create-root (js/document.getElementById "app"))]
    (.render root (r/as-element [app])))

  ;; Ao carregar a página, busca os todos da API
  (get-todos))