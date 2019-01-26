(ns otoch.heart
  (:require [infinitelives.utils.vec2 :as vec2]
            [infinitelives.utils.events :as e]
            [infinitelives.utils.boid :as b]
            [infinitelives.utils.math :as math]
            [infinitelives.utils.spatial :as spatial]
            [infinitelives.utils.sound :as sound]
            [infinitelives.utils.console :refer [log]]
            [infinitelives.pixi.sprite :as s]
            [infinitelives.pixi.canvas :as c]
            [otoch.state :as state]
            [otoch.consts :as consts]
            [otoch.constraints :as constraints]
            [otoch.platforms :as platforms]
            [cljs.core.async :refer [<! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn sin [amp freq n]
  (* amp (Math/sin (* n freq))))

(def x-amp (/ 7 64))
(def x-freq (/ 7 200))
(def y-amp (/ 10 64))
(def y-freq (/ 11 200))
(def size-amp 0.2)
(def size-freq 0.02)
(def size-off 1)

(defn spawn [container position]
  (go
    (c/with-sprite container
      [heart (s/make-sprite :heart :pos (vec2/scale position 64))]
      (loop [n 0]
        (let [[x y] position]
          (s/set-pos! heart
                      (vec2/scale
                       (vec2/vec2
                        (+ x (sin x-amp x-freq n))
                        (+ y (sin y-amp y-freq n))
                        )
                       64))
          #_ (s/set-scale! heart (+ size-off (sin size-amp size-freq n))))
        (<! (e/next-frame))
        (when true
          ;; still alive
          (recur (inc n)))))))
