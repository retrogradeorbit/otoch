(ns otoch.enemy
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
            [otoch.constraints :as constraints]
            [otoch.platforms :as platforms]
            [cljs.core.async :refer [<! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defonce enemies (atom {}))

(defn add! [ekey enemy]
  (swap! enemies assoc ekey enemy))

(defn remove! [ekey]
  (swap! enemies dissoc ekey))

(defn count-enemies []
  (count @enemies))

(defn spawn [container start-pos]
  (go
    (let [ekey (keyword (gensym))
          skey [:enemy ekey]]
      (c/with-sprite container
        [enemy (s/make-sprite :enemy :pos (vec2/scale start-pos 64) :xhandle 0 :yhandle 0)]

        (loop [
               state :walking
               facing :left
               fnum 0
               old-vel (vec2/zero)
               pos start-pos
               ]

          (let [ppos (vec2/scale pos 64)
                old-pos ppos
                x (vec2/get-x pos)
                y (vec2/get-y pos)
                px (vec2/get-x ppos)
                py (vec2/get-y ppos)
                pix (int px)
                piy (int py)
                dx (- px pix)
                dy (- py piy)
                ]
            (s/set-pos! enemy (+ x (* 64 px)) (+ y (* 64 py)))
            (<! (e/next-frame))

            (let [mov-x -0.1

                  platforms-this-frame (platforms/prepare-platforms platforms/platforms fnum)


                  ;; platform subset for player
                  filtered-platforms (platforms/filter-platforms
                                    platforms-this-frame old-pos)

                  ;; simulate a little vertical move down to see if we are
                  ;; standing on solid ground (or a platform)
                  fallen-pos
                  (constraints/constrain-pos constraints/platform-constrain filtered-platforms
                                 old-pos (vec2/add old-pos (vec2/vec2 0 0.1)))
                  ]



              (recur state
                     facing
                     (inc fnum)
                     old-vel
                     ppos))))

        )
      ))
  )
