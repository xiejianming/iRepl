;   Copyright (c) xjm (xiejianming@gmail.com). All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns dbkid)

(defn now
  "Get current time in format <yyyy-MM-dd HH:mm:ss>."
  []
  (.format 
    (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss")
    (.getTime (java.util.Calendar/getInstance))))

(def ^:dynamic *debug-flag* (atom true))

(defn db 
  "Disable/Enable DBK which used to print debug info."
  [& _] 
  (reset! *debug-flag* (not @*debug-flag*)) 
  (println (str "DBK is now "
                (if @*debug-flag*
                  "Enabled!"
                  "Disabled!"))))

(def ^:dynamic *debug-print-length* (atom 5))

(defn set-print-length [^long l] (reset! *debug-print-length* l))

(defmacro dbk
  "Print debug info(universal/global debugger)."
  [& variables]
  `(if (deref *debug-flag*)
    (let [datetime# ~(now)
          naked-msg# (str "Debug: " datetime# " in " ~*file* "@" ~(:line (meta &form)))]
      (if (empty? '~variables)
        (println naked-msg#)
        (binding [*print-length* (deref *debug-print-length*)]
           (let [kvs# (into {} (map vec (partition 2 (interleave 
                                                       (map #(str % " =>") '~variables)
                                                       (vector ~@variables)))))]
             (println (str naked-msg# ":") kvs#)))))))

(defmacro ?
  [& forms]
  (let [prompt #(do (print ">: ") (read-line))]
    (loop [cmd (prompt)]
      (if (= cmd "q")
        (rest &form)
        (do
          (case cmd
            "?" (println (str *file* "@" (:line (meta &form))))
            "l" (println (vec (sort (keys &env))))
            "ll" (println (vec (sort (keys (ns-interns *ns*)))))
            (let [s (read-string cmd) lb (&env s)]
              (print (str cmd ": "))
              (try
                (if lb
                  (println (.eval (.init lb)))
                  (println (eval s)))
                (catch Exception e
                  (println (.getMessage e))))))
          (recur (prompt)))))))

#_(comment
(def ^:dynamic xx 111111)
(binding [xx "this is xx"]
  (let [a 123 b (defn tt[] (println a)) e [1 tt 3 (println 999)]]
    (let [a 789 c "wtf" d (range 5) f {1 2 3 (range 3) 4 (rand)}]
      (println a)
      (println b)
      (println c)
      (println d)
      (? println xx))))
)