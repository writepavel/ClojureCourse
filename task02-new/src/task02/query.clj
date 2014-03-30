(ns task02.query
  (:use [task02 helpers db])
  (:use [clojure.core.match :only (match)]))

;; Функция выполняющая парсинг запроса переданного пользователем
;; 
;; Синтаксис запроса:
;; SELECT table_name [WHERE column comp-op value] [ORDER BY column] [LIMIT N] [JOIN other_table ON left_column = right_column]
;;
;; - Имена колонок указываются в запросе как обычные имена, а не в виде keywords. В
;;   результате необходимо преобразовать в keywords
;; - Поддерживаемые операторы WHERE: =, !=, <, >, <=, >=
;; - Имя таблицы в JOIN указывается в виде строки - оно будет передано функции get-table для получения объекта
;; - Значение value может быть либо числом, либо строкой в одинарных кавычках ('test').
;;   Необходимо вернуть данные соответствующего типа, удалив одинарные кавычки для строки.
;; 
;; - Ключевые слова --> case-insensitive
;;
;; Функция должна вернуть последовательность со следующей структурой:
;;  - имя таблицы в виде строки
;;  - остальные параметры которые будут переданы select
;;
;; Если запрос нельзя распарсить, то вернуть nil

;; Примеры вызова:
;; > (parse-select "select student")
;; ("student")
;; > (parse-select "select student where id = 10")
;; ("student" :where #<function>)
;; > (parse-select "select student where id = 10 limit 2")
;; ("student" :where #<function> :limit 2)
;; > (parse-select "select student where id = 10 order by id limit 2")
;; ("student" :where #<function> :order-by :id :limit 2)
;; > (parse-select "select student where id = 10 order by id limit 2 join subject on id = sid")
;; ("student" :where #<function> :order-by :id :limit 2 :joins [[:id "subject" :sid]])
;; > (parse-select "werfwefw")
;; nil


(defn make-where-function [a b c]
  (let [col (keyword a)
        condition (resolve(symbol b))
        num (parse-int c)]
    #(condition (col %) num)))

;(load-initial-data)
;(select student :where #(= (:year %) 1996))
;(select student :where (make-where-function "year" "=" "1996"))

; Список временно преобразуется в вектор, чтобы элементы добавлялись в конец коллекции
(defn- query
  ([tbl]
     (list tbl))

  ([tbl where1 where2 where3]
     (-> (query tbl)
         (vec)
         (conj (keyword "where") (make-where-function where1 where2 where3))
         (seq)))

  ([tbl where1 where2 where3 lim]
     (-> (query tbl where1 where2 where3)
         (vec)
         (conj (keyword "limit") (parse-int lim))
         (seq)))

  ([tbl where1 where2 where3 order lim]
     (-> (query tbl where1 where2 where3)
         (vec)
         (conj (keyword "order-by") (keyword order) (keyword "limit") (parse-int lim))
         (seq)))

  ([tbl where1 where2 where3 order lim join1 join2 join3]
     (-> (query tbl where1 where2 where3 order lim)
         (vec)
         (conj (keyword "order-by") (keyword order) (keyword "limit") (parse-int lim)
               (keyword "joins")
               ((comp vec list vec list) (keyword join2) join1 (keyword join3)))
         (seq))))

(defn parse-select [^String sel-string]
  (let [check (-> sel-string
                  (.toLowerCase) ;; потому что case-insensitive
                  (.split " ")
                  (vec))]
      (match check
             [ "select" tbl "where" c1 c2 c3 "order" "by" o "limit" lim "join" col1 "on" col2 "=" col3 ]
             (query tbl c1 c2 c3 o lim col1 col2 col3)
             [ "select" tbl "where" c1 c2 c3 "order" "by" o "limit" lim ]
             (query tbl c1 c2 c3 o lim)
             [ "select" tbl "where" c1 c2 c3 "limit" lim ]
             (query tbl c1 c2 c3 lim)
             [ "select" tbl "where" val1 cond val2 ]
             (query tbl val1 cond val2)
             [ "select" tbl ]
             (query tbl)
             :else
             nil)))

;;===========================================================
;; Выполняет запрос переданный в строке.  Бросает исключение если не удалось распарсить запрос

;; Примеры вызова:
;; > (perform-query "select student")
;; ({:id 1, :year 1998, :surname "Ivanov"} {:id 2, :year 1997, :surname "Petrov"} {:id 3, :year 1996, :surname "Sidorov"})
;; > (perform-query "select student order by year")
;; ({:id 3, :year 1996, :surname "Sidorov"} {:id 2, :year 1997, :surname "Petrov"} {:id 1, :year 1998, :surname "Ivanov"})
;; > (perform-query "select student where id > 1")
;; ({:id 2, :year 1997, :surname "Petrov"} {:id 3, :year 1996, :surname "Sidorov"})
;; > (perform-query "not valid")
;; exception...
(defn perform-query [^String sel-string]
  (if-let [_query (parse-select sel-string)]
    (apply select (get-table (first _query)) (rest _query))
    (throw (IllegalArgumentException. (str "Can't parse query: " sel-string)))))
