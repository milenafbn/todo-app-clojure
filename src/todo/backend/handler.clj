(ns todo.backend.handler
  "Este namespace define nossas 'funções de resposta' (Handlers)."
  (:require [todo.backend.db :as db]     ;; Nosso "banco" em memória (db.clj)
            [clojure.string :as str]))   ;; Para a validação do título

(defn hello-handler
  "Nosso primeiro handler. Ele apenas diz 'Olá, Mundo!'"

  [_request] ;; 1. O handler recebe a 'request' como argumento.
             ;;    Usamos '_' para sinalizar que, nesta função,
             ;;    vamos ignorar esse argumento.

  ;; 2. O handler retorna um mapa de 'response'.
  {:status 200            ;; :status 200 é o código HTTP para "OK"
   :body "Hello, World!"}) ;; :body é o conteúdo enviado ao navegador

;; --- Handler para Listar Todos ---
(defn list-todos-handler
  "Handler para a rota GET /api/todos."
  [_request]
  ;; Chama nossa função de banco e a coloca no 'body'
  {:status 200
   :body {:todos (db/get-all-todos)}})

;; --- Handler para Criar um Todo ---
(defn create-todo-handler
  "Handler para a rota POST /api/todos."
  [request]
  ;; :body é um mapa Clojure que o *middleware* wrap-json-body
  ;; cria para nós a partir do JSON que o cliente enviar.
  (let [todo-data (:body request)
        title (:title todo-data)]

    ;; É uma boa prática validar os dados que chegam.
    (if (and title (not (str/blank? title)))
      ;; Sucesso! Os dados são válidos.
      (let [new-todo (db/create-todo todo-data)]
        ;; :status 201 = "Created"
        {:status 201
         :body new-todo})

      ;; Erro de validação.
      {:status 400 ;; "Bad Request"
       :body {:error "O 'título' (title) é obrigatório"}})))