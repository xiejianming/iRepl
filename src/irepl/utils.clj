;   Copyright (c) xjm (xiejianming@gmail.com). All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns irepl.utils
  (:use [clojure.string :only [split-lines]])
  (:use [dbkid]))  

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