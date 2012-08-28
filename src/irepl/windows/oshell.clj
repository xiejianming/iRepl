;   Copyright (c) xjm (xiejianming@gmail.com). All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns irepl.windows.oshell
  (:use [clojure.java.io])
  (:import (java.io InputStreamReader BufferedReader BufferedWriter 
                    OutputStreamWriter PrintWriter))
  (:import (java.util.concurrent TimeUnit)))

(defn- start-os-shell
  "Start an OS shell. This shell could be used to perform responsive tasks. E.g. ping."
  []
  (try
    (let [[shell-name echo-line cmd-separator shell-exit] ["cmd" "echo." "&" "exit"]
          shell (.exec (Runtime/getRuntime) shell-name)
          start-line (str "****" (rand-int 10) "***OUTPUTS START****" (rand-int 10) "***")
          stop-line (str "****" (rand-int 10) "****OUTPUTS END****" (rand-int 10) "****")]
      (and shell
           (let [stdout (BufferedReader. (InputStreamReader. (.getInputStream shell)))
                 stdin (PrintWriter. (BufferedWriter. (OutputStreamWriter. (.getOutputStream shell))))
                 stderr (BufferedReader. (InputStreamReader. (.getErrorStream shell)))
                 enter (fn [^String c] (.println stdin (str c "\r\n")) (.flush stdin))]
             {:exe (fn [^String order & opts]
                     (if (= order shell-exit)
                       nil
                       (do
                         ;; send command to shell
                         (enter (str "echo " start-line cmd-separator
                                     order " " cmd-separator echo-line
                                     cmd-separator "echo " stop-line))
                         ;; get output from shell
                         (loop [line (.readLine stdout)]
                           (if (not= line start-line)
                             (recur (.readLine stdout))))   ;; browse until reach the real content
                         {:out
                          (loop [line (.readLine stdout) out []]
                            (if (= line stop-line)
                              (reduce #(str %1 "\r\n" %2) out)
                              (do
                                (if (not (empty? opts)) (println line))
                                (recur (.readLine stdout) (conj out line)))))
                          :err
                          (let [errs (.available (.getErrorStream shell))]
                            (if (> errs 0)
                              (let [cb (char-array errs)]
                                (and (> (.read stderr cb 0 errs) 0)
                                     (let [e (reduce str cb)]
                                       (if (not (empty? opts)) (println e))
                                       e)))))})))
              :close (fn [] (do 
                              (.close stdout)
                              (.close stdin)
                              (.close stderr)
                              (.waitFor shell)
                              (.destroy shell)))})))
    (catch Exception e
      (.printStackTrace e))
    (finally)))

;; start a shell & make any output (including err) available
(defn- init-osh []  
  (def ^{:private true} oss (start-os-shell))
  (def ^{:private true} oss-do (:exe oss))
  (def ^{:private true} oss-die (:close oss))
  nil)


(defn restart-osh []
  "Restart ish(The OS shell). Please note that all setting on previous 
   ish will be destroyed."
  (try 
    (oss-die)
    (catch Exception e 
      (println)))
  (init-osh))

(defn osh-pwd
  "Get current working directory."
  []
  (first (.split (:out (oss-do "cd")) "\r\n")))

(defn osh
  "Execute OS shell cmd.
   If opts is given then the result will be printed while executing,
   otherwise the cmd will be executed silently. In all cases the result
   of output & err will be returned."
  [^String cmd & opts]
  (apply oss-do cmd opts))

(defn kill-osh
  "Kill the OS shell)."
  []
  (oss-die))