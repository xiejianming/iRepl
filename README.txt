    * Copyright © 2011-2012 xjm (xiejianming@gmail.com)
    *
    * The use and distribution terms for this software are covered by the
    * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
    * By using this software in any fashion, you are agreeing to be bound by
    * the terms of this license.

This is a mimic shell(which I named it 'iRepl') on top of Clojure REPL. It gives 
us a system shell-like (here I have windows cmd.exe) console.

You can also use iRepl as a normal REPL. The basic idea behind iRepl is to
connect the real REPL with OS shell(but yes, it's just a mimic shell), so that we 
can perform some system tasks occasionally.

To start iRepl, do:
    * make sure folder irepl is under your CLASSPATH
    * start Clojure REPL
    * type following in REPL: 
    	* (use 'irepl.core)
    	* (in-ns 'irepl.core)
    	* (irepl)
    	
When iRepl starts, it will show user home directory as the prompt, e.g.:
	"C:\Users\xjm: "

Now you can type "?" to start exploring iRepl.

----------------------华丽分割线 ----------------------
This tool is still under developing and just @ its initial stage. It might be
not as strong/robust/useful as you imagine yet. If you get any ideas about 
this tool(bug, functionality etc.), please don't hesitate to let me know
(email: xiejianming@gmail.com).
----------------------华丽分割线----------------------