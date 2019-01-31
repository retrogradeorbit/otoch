(ns otoch.controls
  (:require [infinitelives.utils.events :as e]
            [infinitelives.utils.gamepad :as gp]
            [infinitelives.utils.vec2 :as vec2]
            ))

(defn start-game? []
  (or
   (e/any-pressed?)
   (gp/button-pressed? :a)
   (gp/button-pressed? :b)
   (gp/button-pressed? :x)
   (gp/button-pressed? :y)
   (gp/button-pressed? :start)))

(defn jump-button-pressed? []
  (or
   (e/is-pressed? :z)
   (e/is-pressed? :space)
   (e/is-pressed? :comma)
   (gp/button-pressed? 0 :x)
   (gp/button-pressed? 0 :y)))

(defn throw-rune-pressed? []
  (or
   (e/is-pressed? :x)
   (e/is-pressed? :.)
   (gp/button-pressed? 0 :a)
   (gp/button-pressed? 0 :b)))

(defn get-joy-vec []
  (vec2/vec2 (cond (e/is-pressed? :left) -1
                   (e/is-pressed? :a) -1
                   (e/is-pressed? :right) 1
                   (e/is-pressed? :d) 1
                   :default (or (gp/axis 0) 0))
             (cond (e/is-pressed? :up) -1
                   (e/is-pressed? :w) -1
                   (e/is-pressed? :down) 1
                   (e/is-pressed? :s) 1
                   :default (or (gp/axis 1) 0))
                 ))
