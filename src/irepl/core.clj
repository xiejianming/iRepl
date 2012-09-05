;   Copyright (c) xjm (xiejianming@gmail.com). All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns irepl.core
  (:use [dbkid])
  (:use [irepl.utils])
  (:use [clojure.string :only [triml split replace-first blank?]])
  (:use [irepl.init]) 
  (:use [clojure.repl]))

(defn ask
  "Ask for an input."
  ([] (ask "Please Input"))
  ([^String prompt]
    (printf "%s: " prompt)
    (flush)
    (read-line)))
  
(defn- internal?
  "Check to see if a given cmd is internal commands or not."
  [name]
  (@*builtins* name))

(defn- exec-internal
  "Execute cmd as internal(this iREPL call) program."
  [name opts]
  (let [f (@*builtins* name)]
    (dbk f (internal? name) opts)
    (if (:macro (meta f))
      (eval (@f nil nil opts))
      (f opts))))

(defn- exec-clj
  "Execute Clojure codes."
  [^String cmd]
  (try
    (let [o-form (read-string cmd)]
      ;(dbk "====NORMAL====")
      (eval o-form))
    (catch Exception e
      (let [idn "EOF while reading"
            msg (subs (.getMessage e) 0 17)]
        (if (= idn msg)
          (exec-clj (str cmd "\n" (.readLine *in*)))
          (throw e))))))

(defn- exec [^String cmd & opts]  
  (let [[name & _] (break-str cmd)]
    (if (internal? name)
      (exec-internal name (triml (subs cmd (count name))))
      (if (= \! (first name))
        (if (empty? opts)
          (do-external (subs (triml cmd) 1))
          (exec-external (subs (triml cmd) 1)))
        (if (empty? opts)
          (println (exec-clj cmd))
          (exec-clj cmd))))))

(defn- parse
  "Extract result from OS/Clojure call \"xxx\" within operator \"$${xxx}\"."
  [^String input]
  (let [prtn #"\$\$\{.*?\}"]
    (loop [strn input]
      (let [found (re-find prtn strn)]
        (if (not found)
          strn
          (let [sub-cmd (triml (subs found 3 (dec (count found))))
                result (if (blank? sub-cmd) "" (exec sub-cmd :silent))]
            (recur (.replace strn found 
                     (if result (.toString result) "")))))))))
    

(defn irepl
  "Starts iRepl."
  [& p]
  (println "
  ****/////////////////////////////////////////////////////////////
  ****
  ****      Welcome to iRepl. Please type \"?\" to start..... 
  ****
  ****/////////////////////////////////////////////////////////////
")  
  (let [prompt (if (empty? p)
                 "iREPL"
                 (first p))
        exit-cmd #{"8" "88" "quit" "q" "Q" "bye"}]
    (init)
    (loop [input (ask (get-current-path))]
      (let [cmd (parse input)]
        (println)
        (if (blank? cmd)
          (recur (ask (str \newline (get-current-path))))
          (if (exit-cmd cmd)
            (do 
              (clean-up)
              (println "Quiting iRepl...."))
            (do 
              (try 
                (exec cmd)
                (catch Exception e 
                  #_(.printStackTrace e)
                  (println (.toString e))))
              (recur (ask (str \newline (get-current-path)))))))))))

#_(do (use 'irepl.core :reload) (in-ns 'irepl.core) (irepl))