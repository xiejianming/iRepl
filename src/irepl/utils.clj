;   Copyright (c) xjm (xiejianming@gmail.com). All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns irepl.utils)  

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

(defmacro add
  "Add k-v into a map."
  [m k v]
  `(swap! ~m #(assoc % ~k ~v)))

(defmacro macro?
  "To check if a given symbol presents a macro or not."
  [s]
  `(:macro (meta #'~s)))

;;<<<<<<<<<< debugger <<<<<<<<<<
(defn now
  "Get current time in format <yyyy-MM-dd HH:mm:ss>."
  []
  (.format 
    (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss")
    (.getTime (java.util.Calendar/getInstance))))

(def ^:dynamic *debug-flag* true)
(def ^:dynamic *debug-print-length* 5)

(defmacro gdb
  "Print debug info(universal/global debugger)."
  [& variables]
  (if *debug-flag*
    (let [datetime (now)
          naked-msg (str "Debug: " datetime " in " *file* "@" (:line (meta &form)))]
      (if (empty? variables)
        (println naked-msg)
        `(binding [*print-length* *debug-print-length*]
           (let [kvs# (into {} (map vec (partition 2 (interleave 
                                                       (map #(str % " =>") '~variables)
                                                       (vector ~@variables)))))]
             (println (str ~naked-msg ":") kvs#)
             ;(last (first kvs#))
             ))))))

;;>>>>>>>>>> debugger >>>>>>>>>>