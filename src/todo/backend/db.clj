(ns todo.backend.db
  "Este namespace gerencia os dados dos 'todos'
   usando um banco de dados persistente SQLite."
  (:require [next.jdbc :as jdbc]))

;; --- 1. A Configuração (Especificação do Banco) ---
;; O banco será salvo em um arquivo chamado "prod.db",
;; criado automaticamente na raiz do projeto.
(def db-spec {:dbtype "sqlite"
              :dbname "prod.db"})

;; --- 2. Função de Inicialização ---
;; Cria a tabela "todos" se ela ainda não existir.
;; Vamos chamá-la no 'core.clj' quando o servidor iniciar.
(defn initialize-database!
  "Cria a tabela 'todos' no banco de dados se ela não existir."
  []
  (jdbc/execute! db-spec ["
    CREATE TABLE IF NOT EXISTS todos (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      title TEXT,
      description TEXT,
      completed BOOLEAN DEFAULT 0,
      created_at TEXT
    )
  "]))

;; --- 3. As Novas Funções (substituindo os 'atoms') ---

(defn get-all-todos
  "Retorna uma lista com todos os 'todos' no banco."
  []
  ;; O 'execute!' roda o SQL e já retorna mapas Clojure!
  (jdbc/execute! db-spec ["SELECT * FROM todos ORDER BY created_at DESC"]))

(defn create-todo
  "Cria um novo 'todo', salva no banco e o retorna."
  [todo-data] ;; ex: {:title "Minha tarefa", :description "..."}
  ;; A cláusula 'RETURNING *' pede ao SQLite para devolver
  ;; a linha recém-inserida (com o id gerado, o completed, etc.).
  ;; 'execute-one!' retorna esse único resultado como um mapa.
  (jdbc/execute-one! db-spec ["
    INSERT INTO todos (title, description, completed, created_at)
    VALUES (?, ?, ?, ?)
    RETURNING *"
    (:title todo-data)
    (:description todo-data)
    0 ;; completed = falso (lembre-se: SQLite usa 0/1)
    (str (java.time.Instant/now))]))

(defn toggle-todo!
  "Alterna o status 'completed' de um todo no banco."
  [id]
  ;; (1 - completed) é um truque SQL para inverter: 0 -> 1 e 1 -> 0
  ;; RETURNING * devolve a linha já atualizada (ou nada, se o id não existir)
  (jdbc/execute-one! db-spec ["
    UPDATE todos
    SET completed = (1 - completed)
    WHERE id = ?
    RETURNING *"
    id]))

(defn delete-todo!
  "Remove um todo do banco. Retorna o todo removido, ou nil se não existir."
  [id]
  ;; RETURNING * aqui devolve a linha que acabou de ser apagada
  ;; Serve para o handler saber se o id existia (nil = não existia)
  (jdbc/execute-one! db-spec ["
    DELETE FROM todos
    WHERE id = ?
    RETURNING *"
    id]))
