(ns shell.irepl
  (:use [utils.debugger :only [gdb]])
  (:use [shell.utils])
  (:use [clojure.string :only [triml]])
  (:use [shell.init]))

(defn ask
  "Ask for an input."
  ([] (ask "Please Input"))
  ([^String prompt]
    (printf "%s: " prompt)
    (flush)
    (.readLine *in*)))
  
(defn- internal?
  "Check to see if a given cmd is internal commands or not."
  [name]
  (@*builtins* name))

(defn- exec-internal
  "Execute cmd as internal(this iREPL call) program."
  [name opts]
  (let [f (@*builtins* name)]
    ;(gdb "====INTERNAL====" (type f))
    (if (:macro (meta f))
      (eval (@f nil nil opts))
      (f opts))))

(defn- exec-clj
  "Execute Clojure codes."
  [^String cmd]
  (try
    (let [o-form (read-string cmd)]
      ;(gdb "====NORMAL====")
      (println (eval o-form)))
    (catch Exception e
      (let [idn "EOF while reading"
            msg (subs (.getMessage e) 0 17)]
        (if (= idn msg)
          (exec-clj (str cmd "\n" (.readLine *in*)))
          (throw e))))))

(defn- exec [^String cmd]  
  (let [[name & _] (break-str cmd)]
    (if (internal? name)
      (exec-internal name (.trim (subs cmd (count name))))
      (if (= \! (first name))
        (exec-external (subs (triml cmd) 1))
        (exec-clj cmd)))))

(defn shell
  "Starts the shell."
  [& p]
  (let [prompt (if (empty? p)
                 "iREPL"
                 (first p))
        exit-cmd #{"8" "88" "quit" "q" "Q" "bye"}]
    (init)
    (loop [cmd (ask (get-current-path))]
      (if (empty? (.trim cmd))
        (recur (ask (get-current-path)))
        (if (exit-cmd cmd)
          (do 
            (clean-up)
            (println "Quiting iREPL...."))
          (do 
            (try 
              (exec cmd)
              (catch Exception e
                (println (.getMessage e))))
            (recur (ask (get-current-path)))))))))

#_(do (use 'shell.irepl) (in-ns 'shell.irepl) (shell))