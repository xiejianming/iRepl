;   Copyright (c) xjm (xiejianming@gmail.com). All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns irepl.windows.core
  (:use [dbkid])
  (:use [clojure.java.io :only [file]])
  (:use [irepl.utils])
  (:use [irepl.init])
  (:use [irepl.windows.oshell])
  (:use [clojure.string :only [triml]]))

;;<<<<<<<<<< system shell env <<<<<<<<<<
(reset! *shell_env* {})
(defn- get-shell-env
  "Get shell env variables."
  []
  (let [env (clojure.string/split-lines  
              (:out (clojure.java.shell/sh "cmd" "/c" "set")))]
    (reduce #(conj %1 (vec (.split %2 "="))) {} env)))

(reset! *shell_env* (get-shell-env))

(defn cmd-set
  "To display/set/unset env variables(a mimic set command in cmd.exe).
   Usage: set [name[=[value]]]
     - a \"set\" without any parameter will print all env variables;
     - \"set name\" will print variables which starts with \"name\"(case insensitive);
     - \"set name=\" will unset the variable;
     - \"set name=value\" will set env variable \"name\", in which:
           - any form of \"%v-name%\" will be replace by their value respectively.
   Note: any spaces on the right most will also be treated as part of variable value."
  [^String opts]
  (if (empty? (.trim opts))
    (loop [m (sort @*shell_env*)]
      (if m
        (do
          (let [[k v] (first m)]
            (println (str k "=" v)))
          (recur (next m)))))
    (let [idx (.indexOf opts "=")]
      (if (= idx -1)
        (let [s (filter #(= (.indexOf (.toLowerCase (first %)) (.toLowerCase opts)) 0) @*shell_env*)]          
          (if (empty? s)
            (println (str "Environment variable " opts " not defined."))
            (loop [ss s]
              (if-let [[[k v]] ss]
                (do
                  (println (str k "=" v))
                  (recur (next ss)))))))
        (let [name (subs opts 0 idx)
              value (subs opts (inc idx))]
          (if (empty? value)
            (-env name)
            (+env name
                  (clojure.string/replace value 
                                          #"%\w+%" 
                                          #(let [k (subs %1 1 (dec (count %1)))]
                                             (@*shell_env* k ""))))))))))
    
(+internal set cmd-set)

;;>>>>>>>>>> system shell env >>>>>>>>>>
               

(defn cd 
  "Change directory.
   Usage: cd [/] [\\] [path] [relative-path]
     - \"cd\" without any parameters will print current directory;
     - \"cd /\" or \"cd \\\" will change to root of corresponding drive;
     - \"cd path\" will change to given path;
     - \"cd relative-path\" will change to relative path.     
   If the given path is not correct, an error msg will be printed."
  [^String opts]
  (let [p (.trim opts)]
    (case p
      "" (pwd)
      "/" (do (set-wd (subs (get-current-path) 0 3)) (pwd))
      "\\" (do (set-wd (subs (get-current-path) 0 3)) (pwd))
      (let [tmp-f (file (str (get-current-path) "/" p))]
        (dbk 1 tmp-f)
        (if (.isDirectory tmp-f)
          (set-wd (.getCanonicalPath tmp-f))
          (let [tmp-f (file p)]
            (dbk 2 tmp-f)
            (if (.isDirectory tmp-f)
              (set-wd (.getCanonicalPath tmp-f))
              (println "Incorrect path name: " p))))
        (pwd)))))

(+internal cd cd)

(defn ls
  "List all files under current directory."
  [^String opts]
  (prnt-external-result 
    (exec-external 
      (construct-str-cmd "dir" (break-str opts)))))

(+internal ls ls)
(+internal dir ls)

#_(defn ext!
  "Toggle sh-mode or execute external cmd \"xxx\" directly(no popup).
**NOTE**: interactive task will hang the whole programe!!!"
  [^String s]
  (restart-osh)
  (if (empty? s)  
    (let [ask (fn [] 
                (print (str (osh-pwd) "[SH]> ")) 
                (.flush *out*) 
                (triml (read-line)))]
  (println "
  ****/////////////////////////////////////////////////////////////
  ****
  **** DON'T perform any interactive task!!! This will KILL iRepl. 
  ****
  ****/////////////////////////////////////////////////////////////
")
      (loop [cmd (ask)]
        (if (not= cmd "!!")
          (do
            (osh cmd :print)
            (recur (ask))))))
    (osh s :print))
  (kill-osh))

#_(+internal !! ext!)

(defn open-cmd
  "Open a cmd window or execute external cmd \"xxx\" in a popup window."
  [^String s]
  (exec-external "start cmd"))

(+internal ! open-cmd)

#_(defn cmd-ping
  "Cmd \"ping\" wrapper. See \"!ping /?\"."
  [^String s]
  (ext! (str "ping " s)))

#_(+internal ping cmd-ping)

#_(defn cmd-tasklist
  "Cmd \"tasklist\" wrapper. See \"? tasklist\""
  [^String s]
  (do-external (str "tasklist " s)))
#_(+internal ps cmd-tasklist)