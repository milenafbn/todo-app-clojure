(ns todo.backend.handler
  "Este namespace define nossas 'funções de resposta' (Handlers).")

(defn hello-handler
  "Nosso primeiro handler. Ele apenas diz 'Olá, Mundo!'"

  [_request] ;; 1. O handler recebe a 'request' como argumento.
             ;;    Usamos '_' para sinalizar que, nesta função,
             ;;    vamos ignorar esse argumento.

  ;; 2. O handler retorna um mapa de 'response'.
  {:status 200            ;; :status 200 é o código HTTP para "OK"
   :body "Hello, World!"}) ;; :body é o conteúdo enviado ao navegador