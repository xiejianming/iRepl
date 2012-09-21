;   Copyright (c) xjm (xiejianming@gmail.com). All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns irepl.utils
  (:use [clojure.string :only [split-lines]])
  (:use [dbkid]))  

(defn ask
  "Ask for an input."
  ([] (ask "Please Input"))
  ([^String prompt]
    (printf "%s: " prompt)
    (flush)
    (read-line)))

(defn construct-str-cmd
  "To construct a string cmd with space as separator."
  [name & opts] 
  (.trim 
    (reduce str name
            (apply 
              interleave (repeat (count opts) " ")
              opts))))

(defn break-str
  "Put non-space string into array."
  [^String str]
  ;(filter #(not= "" %) (.split str " "))
  (.split (.trim str) "\\s+"))

(defn get-os-name []
  "Return the host OS name."
  (.toLowerCase 
    (first (.split (System/getProperty "os.name") " "))))

(defmacro plus
  "Add k-v into a map atom."
  [m k v]
  `(swap! ~m #(assoc % ~k ~v)))
(defmacro minus
  "Remove k-v from a map atom."
  [m k]
  `(swap! ~m #(dissoc % ~k)))

(defmacro macro?
  "To check if a given symbol presents a macro or not."
  [s]
  `(:macro (meta #'~s)))

;; >>>>>>>>>>>>> printing >>>>>>>>>>>>>>>>>
(defn- rfill2
  [^String s len] 
  (apply str s (repeat (- len (count s)) " ")))
(defn- lfill2
  [^String s len] 
  (str 
    (loop [i (- len (count s)) spaces ""] 
      (if (= i 0)
        spaces
        (recur (dec i) (str spaces " "))))
    s))
  
(defn width-prn
  [items]
  (let [max-len (apply max (map count items))
        cols (quot 80 (+ max-len 2))
        sis (map #(rfill2 % max-len) (sort items))
        line (apply str (repeat 80 "-"))]
    (println (str 
               (apply str line \newline
                      (interleave sis (cycle (loop [i (dec cols) vs []]
                                               (if (= i 0)
                                                 (conj vs \newline)
                                                 (recur (dec i) (conj vs " ")))))))
               \newline line))))

;; <<<<<<<<<<<<< printing <<<<<<<<<<<<<<<<<