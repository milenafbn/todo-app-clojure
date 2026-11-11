(ns todo.backend.db
  "Este namespace gerencia os dados dos 'todos' em memória.")

;; (def) cria uma 'var' global.
;; (atom {}) cria nossa "caixa" (atom) e coloca
;; um mapa imutável vazio {} dentro dela.
(def todos-db (atom {}))
;; Nosso banco terá a forma: {1 {:id 1, :title "..."}, 2 {:id 2, ...}}

;; Uma "caixa" separada para o contador de IDs.
;; Começa em 0: cada novo todo recebe o valor já incrementado (1, 2, 3...).
(def next-id (atom 0))

;; --- Nossas Funções de Acesso ao Banco ---

(defn get-all-todos
  "Retorna uma lista com todos os 'todos' no banco."
  []
  ;; @todos-db: "olha dentro" da caixa (lê o valor).
  ;; (vals): pega apenas os valores do mapa (ignora as chaves/IDs).
  (vec (vals @todos-db)))

(defn create-todo
  "Cria um novo 'todo', salva no banco e o retorna."
  [todo-data] ;; ex: {:title "Minha tarefa"}
  (let [;; 1. Gera o próximo ID: swap! incrementa a caixa e DEVOLVE
        ;;    o novo valor, tudo em um único passo atômico.
        id (swap! next-id inc)

        ;; 2. Cria um NOVO mapa imutável com os dados completos
        new-todo (assoc todo-data
                        :id id
                        :completed false
                        :created-at (str (java.time.Instant/now)))]

    ;; 3. (swap!): "troca" o conteúdo da caixa 'todos-db',
    ;;    aplicando 'assoc' ao mapa antigo para criar um novo.
    (swap! todos-db assoc id new-todo)

    ;; 4. Retorna o 'todo' recém-criado.
    new-todo))
