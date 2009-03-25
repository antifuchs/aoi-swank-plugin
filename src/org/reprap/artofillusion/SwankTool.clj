;;; Copyright (c) Andreas Fuchs. All rights reserved.
;;; The use and distribution terms for this software are covered by the
;;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;; which can be found in the file epl-v10.html at the root of this distribution.
;;; By using this software in any fashion, you are agreeing to be bound by
;;; the terms of this license.
;;; You must not remove this notice, or any other, from this software.


(ns org.reprap.artofillusion.SwankTool
  (:require swank.swank clojure.main)
  (:gen-class
   :implements [artofillusion.ModellingTool]
   :prefix "tool-"))

(defn tool-getName [this]
  "Start Swank REPL")

(defn tool-commandSelected [this window]
  (clojure.main/with-bindings 
   (swank.swank/ignore-protocol-version "2009-03-25") 
   (swank.swank/start-server "/dev/null" :port 4006 :encoding "iso-latin-1-unix")))