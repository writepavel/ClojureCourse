(ns dsl.core
  (:use clojure.walk)
  (:require [clj-time.core :as t]
            [clj-time.coerce :refer [from-date to-date]]))

(def cal (java.util.Calendar/getInstance))
(def today (java.util.Date.))
(def yesterday (do (.add cal java.util.Calendar/DATE -1) (.getTime cal)))
(def tomorrow (do (.add cal java.util.Calendar/DATE 2) (.getTime cal)))


(defn one [] 1)




;; Поддерживаемые операции:
;; > >= < <=
;; Функция принимает на вход три аргумента. Она должна определить,
;; являются ли второй и третий аргумент датами. Если являются,
;; то из дат необходимо взять date.getTime и сравнить их по этому числу.
;; Если получены не даты, то выполнить операцию op в обычном порядке:
;; (op d1 d2).
(defn d-op [op d1 d2]
  (if (and (instance? java.util.Date d1)
           (instance? java.util.Date d2))
    (let [time1 (.getTime d1)
          time2 (.getTime d2)]
      (op time1 time2))
    (op d1 d2)))

(d-op > tomorrow yesterday)

;; Пример вызова:
;; (d-add today '+ 1 'day)
;; Функция должна на основе своих параметров создать новую дату.
;; Дата создается при помощи календаря, например так:
;; (def cal (java.util.Calendar/getInstance))
;; (.add cal java.util.Calendar/DATE 2)
;; (.getTime cal)
;; Во-первых, необходимо на основе 'op' и 'num' определить количество, на
;; которое будем изменять дату. 'Op' может принимать + и -, соответственно
;; нужно будет не изменять либо изменить знак числа 'num'.
;; Во-вторых, необходимо узнать период, на который будем изменять дату.
;; Например, если получили 'day, то в вызове (.add cal ...) будем использовать
;; java.util.Calendar/DATE. Если получили 'months, то java.util.Calendar/MONTH.
;; И так далее.
;; Результат работы функции - новая дата, получаемая из календаря так: (.getTime cal)

;; http://clj-time.github.io/clj-time/doc/clj-time.core.html#var-day
;; some periods are used from clj-time!
(defn d-add [date op num period]
  (let [_op (case op
              + t/plus
              - t/minus)
        period-name (case period
                      week t/weeks
                      weeks t/weeks
                      day t/days
                      days t/days
                      month t/months
                      months t/months
                      minute t/minutes
                      minutes t/minutes
                      year t/years
                      years t/years
                      hour t/hours
                      hours t/hours
                      second t/seconds
                      seconds t/seconds)
        _period (period-name num)
        clj-date (from-date date)]
    (to-date (_op clj-date _period))))

(d-add tomorrow '+ 1 'week)

;; Можете использовать эту функцию для того, чтобы определить,
;; является ли список из 4-х элементов тем самым списком, который создает новую дату,
;; и который нужно обработать функцией d-add.
(defn is-date-op? [code]
  (when (seq? code) (let [op (second code)
         period (last code)]
     (and (= (count code) 4)
          (or (= '+ op)
              (= '- op))
          (contains? #{'day 'days 'week 'weeks 'month 'months 'year 'years
                       'hour 'hours 'minute 'minutes 'second 'seconds} period )))))

(defn is-date-compare-op? [code]
  (and (list? code)
       (= (count code) 3)
       (contains? #{'> '< '>= '<=} (first code))))

;; В code содержится код-как-данные. Т.е. сам code -- коллекция, но его содержимое --
;; нормальный код на языке Clojure.
;; Нам необходимо пройтись по каждому элементу этого кода, найти все списки из 3-х элементов,
;; в которых выполняется сравнение, и подставить вместо этого кода вызов d-op;
;; а для списков из четырех элементов, в которых создаются даты, подставить функцию d-add.

(defn- addition-dates-replacer [code]
  ;; (println "_additon_replacer. Check for code " code " is " (is-date-op? code))
  (if (is-date-op? code)
    (let [date (first code)
          op (second code)
          num (nth code 2)
          period (last code)]
      `(d-add ~date '~op ~num '~period))
    code))

(defn- compare-dates-replacer [code]
  (if  (is-date-compare-op? code)
    (let [op (first code)
          d1 (second code)
          d2 (last code)]
      `(d-op ~op ~d1 ~d2))
    code))

 (defmacro with-datetime [& code]
   `(do ~@(postwalk
           (comp
            addition-dates-replacer
            compare-dates-replacer) code)))

 ;; (defmacro with-datetime [& code]
 ;;   `(do ~@(postwalk
 ;;           addition-dates-replacer code)))

 ;; (defmacro with-datetime [& code]
 ;;   `(do ~@(postwalk addition-dates-replacer code)))

;; (defmacro with-datetime [& code]
;;    `(do ~@(prewalk-demo code)))


(macroexpand-1 '(with-datetime (> today yesterday)))

(macroexpand-1 '(with-datetime (tomorrow + 1 week)))

(with-datetime   (> today yesterday))

(with-datetime (tomorrow + 1 week))

;; Примеры вызова
(with-datetime
  (if (> today tomorrow) (println "Time goes wrong"))
  (if (<= yesterday today) (println "Correct"))
  (let [six (+ 1 2 3)
        d1 (today - 2 days)
        d2 (today + 1 week)
        d3 (today + six months)
        d4 (today + (one) year)]
    (if (and (< d1 d2)
             (< d2 d3)
             (< d3 d4))
      (println "DSL works correctly"))))
