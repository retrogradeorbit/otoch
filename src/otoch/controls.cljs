(ns otoch.controls
  (:require [infinitelives.utils.events :as e]
            [infinitelives.utils.gamepad :as gp]
            [infinitelives.utils.vec2 :as vec2]
            ))

(defn jump-button-pressed? []
  (or
   (e/is-pressed? :z)
   (e/is-pressed? :space)
   (gp/button-pressed? 0 :x)))

(defn throw-rune-pressed? []
  (or
   (e/is-pressed? :x)
   (gp/button-pressed? 0 :y)))

(defn get-joy-vec []
  (vec2/vec2 (or (gp/axis 0)
                 (cond (e/is-pressed? :left) -1
                       (e/is-pressed? :right) 1
                       :default 0) )
             (or (gp/axis 1)
                 (cond (e/is-pressed? :up) -1
                       (e/is-pressed? :down) 1
                       :default 0)
                 )))
