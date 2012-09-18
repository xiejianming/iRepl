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

(defn- str-repl
  "A mimic repl read code from given string. A vector containing results will be returned."
  [^String s]
  (with-open [in (-> (java.io.StringReader. s) clojure.lang.LineNumberingPushbackReader.)]
    (loop [rs []]
      (let [v (eval (read in))]
        (if (= v :__let-me-out)
          rs
          (recur (conj rs v)))))))

(declare parse)

(defn- exec-clj
  "Execute Clojure codes."
  [^String cmd]
  (try
    #_(let [o-form (read-string (str "(do " (parse cmd) ")"))]
      (eval o-form))
    (let [rs (str-repl (str (parse cmd) " :__let-me-out"))]
      (if (> (count rs) 1)
        rs
        (first rs)))
    (catch Exception e
      (let [idn "EOF while reading"
            msg (.getMessage e)]        
        (if (not= (.indexOf msg idn) -1)
          (exec-clj (str cmd \newline (.readLine *in*)))
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
  ****      Welcome to iRepl. You could type \"?\" to start..... 
  ****
  ****/////////////////////////////////////////////////////////////
")  
  (let [prompt (if (empty? p)
                 "iREPL"
                 (first p))
        exit-cmd #{"8" "88" "quit" "q" "Q" "bye"}]
    (init)
    (loop [cmd (ask (get-current-path))]
      ;(println)
      (if (blank? cmd)
        (recur (ask (str \newline (get-current-path))))
        (if (exit-cmd cmd)
          (do 
            (clean-up)
            (println "Quiting iRepl...."))
          (do 
            (try 
              (exec (parse cmd))
              (catch AssertionError e
                (println (.toString e)))
              (catch Exception e 
                #_(.printStackTrace e)
                (println (.toString e))))
            (recur (ask (str \newline (get-current-path))))))))))

#_(do (use 'irepl.core :reload) (in-ns 'irepl.core) (irepl))