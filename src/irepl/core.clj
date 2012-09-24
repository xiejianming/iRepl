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

(defn str-repl
  "A mimic repl read code from given string. 
A vector containing unevaluated forms will be returned."
  [^String s]
  (if (empty? s) 
    ""
    (let [s (str s " :__let-me-out")]
      (with-open [in (-> (java.io.StringReader. s) 
                       clojure.lang.LineNumberingPushbackReader.)]
        (loop [rs []]
          (let [o (read in)]
            (if (= o :__let-me-out)
              rs
              (recur (conj rs o)))))))))
  
(declare parse)

(defn- exec-clj
  "Execute Clojure codes."
  [^String cmd]
  (try
    #_(let [o-form (read-string (str "(do " (parse cmd) ")"))]
      (eval o-form))
    (let [os (str-repl (parse cmd))
          rs (map #(let [v (eval %)] (set! *3 *2) (set! *2 *1) (set! *1 v)) os)]
      (if (> (count rs) 1)
        (vec rs)
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
    
(defn explain
  "Explain given string."
  [^String cmd]
  (exec (parse cmd) :silent))

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
  (let [exit-cmd #{"8" "88" "quit" "q" "Q" "bye"}]
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
              (catch Throwable e 
                #_(.printStackTrace e)
                (println (.toString e))
                (set! *e e)))
            (recur (ask (str \newline (get-current-path))))))))))

#_(do (use 'irepl.core :reload) (in-ns 'irepl.core) (irepl))