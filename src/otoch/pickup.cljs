(ns otoch.pickup
  (:require [infinitelives.utils.vec2 :as vec2]
            [infinitelives.utils.events :as e]
            [infinitelives.utils.boid :as b]
            [infinitelives.utils.math :as math]
            [infinitelives.utils.sound :as sound]
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

(defn set-pos [sprite x y n]
  (s/set-pos! sprite (* 64 (+ x (* 0.025 (Math/sin (/ n 7))))) (* 64 (+ y (* 0.1 (Math/sin (/ n 11)))))))

(defn spawn [container texture start-frame start-pos]
  (go
    (c/with-sprite container
      [pickup (s/make-sprite texture :pos (vec2/scale start-pos 64)
                             :scale .5)]
      (loop [n 0
             p start-pos
             v (vec2/zero)
             ]
        (let [[x y] p]
          (set-pos pickup x y n))
        (<! (e/next-frame))
        (let [player-pos (:pos @state/state)
              distance-squared (vec2/magnitude-squared (vec2/sub player-pos p))
              ]
          (if (> distance-squared 0.1)
            ;; still alive
            (let [platform-state
                  (-> platforms/platforms
                      (platforms/prepare-platforms n)
                      (platforms/filter-platforms (vec2/zero)))

                  new-pos
                  (constraints/constrain-pos
                   constraints/pickup-constrain
                   (-> platforms/platforms
                       (platforms/prepare-platforms (+ 1 start-frame n))
                       (platforms/filter-platforms p))
                   p (vec2/add p v))

                  new-vel (-> new-pos
                              (vec2/sub p)

                              (vec2/add consts/gravity)
                              (vec2/scale 0.99))

                  vel-x (vec2/get-x new-vel)
                  ]

              (recur (inc n)
                     new-pos
                     new-vel))

            ;; picked up
            (do
              (swap! state/state update :runes inc)
              (sound/play-sound :collect 0.5 false))))))))
