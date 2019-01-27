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
            [otoch.consts :as consts]
            [otoch.constraints :as constraints]
            [otoch.platforms :as platforms]
            [cljs.core.async :refer [<! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defonce enemies (atom {}))

#_ @enemies

(defn add! [ekey enemy]
  (swap! enemies assoc ekey enemy))

(defn remove! [ekey]
  (swap! enemies dissoc ekey))

(defn count-enemies []
  (count @enemies))

(defn collided?
  "given the players pos, find the first collided enemy"
  [player-pos]
  (->>
   (for [[k {:keys [pos]}] @enemies]
     (let [distance-squared (vec2/magnitude-squared (vec2/sub player-pos pos))]
       (when (< distance-squared 0.5)
         k)))
   (map identity)
   first))

(def reverse-dir
  {:left :right
   :right :left})

(def speed 0.0005)

(defn wobble [y n]
  (+ y (* 4 (Math/sin (/ n 12))))
  )

(defn spawn [container start-pos sprite]
  (go
    (let [ekey (keyword (gensym))
          start-frame 0]
      (c/with-sprite container
        [enemy (s/make-sprite sprite :pos (vec2/scale start-pos 64))]
        (swap! enemies assoc ekey {:pos (vec2/scale start-pos 64)} )
        (loop [n 0
               p start-pos
               v (vec2/zero)
               facing :left
               ]
          (let [ppos (vec2/scale p 64)
                [x y] ppos
                y (if (= sprite :enemy-1) (wobble y n) y)
                ]
            (swap! enemies assoc-in [ekey :pos] (vec2/scale (vec2/vec2 x y) (/ 1 64)))
            (s/set-pos! enemy x y))
          (s/set-scale! enemy (if (= :left facing) 1 -1) 1)
          (<! (e/next-frame))
          (if true ;;(< n 3000)
            ;; still alive
            (do
              #_ (let [frame (int (/ n 60))
                       texture :rune-1]
                   (s/set-texture! sprite (t/get-texture texture)))

              (let [platform-state
                    (-> platforms/platforms
                        (platforms/prepare-platforms n)
                        (platforms/filter-platforms (vec2/zero)))

                    new-pos
                    (constraints/constrain-pos
                     constraints/enemy-constrain
                     (-> platforms/platforms
                         (platforms/prepare-platforms (+ 1 start-frame n))
                         (platforms/filter-platforms p))
                     p (vec2/add (vec2/add p v) (vec2/vec2 (if (= :left facing) (- speed) speed) 0)))

                    new-vel (-> new-pos
                                (vec2/sub p)

                                (vec2/add consts/gravity)
                                (vec2/scale 0.98))
                    vel-x (vec2/get-x new-vel)
                    new-facing (if (zero? vel-x) (reverse-dir facing) facing)
                    ]

                (recur (inc n)
                       new-pos
                       new-vel
                       new-facing)))

            ;; rune over. return final pos
            p))



        #_ (loop [
                  state :walking
                  fnum 0
                  old-vel (vec2/zero)
                  ppos start-pos
                  facing :left
                  ]

             (let [
                   old-pos ppos
                   pos ppos
                   x (vec2/get-x pos)
                   y (vec2/get-y pos)
                   px (vec2/get-x ppos)
                   py (vec2/get-y ppos)
                   pix (int px)
                   piy (int py)
                   dx (- px pix)
                   dy (- py piy)
                   ]
               (js/console.log pos (+ x (* 64 px)) (+ y (* 64 py)))
               (s/set-pos! enemy (+ x (* 64 px)) (+ y (* 64 py)))
               (<! (e/next-frame))




               #_ (let [joy-dx -0.01
                        joy-dy 0.0

                        platforms-this-frame (platforms/prepare-platforms platforms/platforms fnum)


                        ;; platform subset for player
                        filtered-platforms (platforms/filter-platforms
                                            platforms-this-frame old-pos)

                        ;; simulate a little vertical move down to see if we are
                        ;; standing on solid ground (or a platform)
                        fallen-pos
                        (constraints/constrain-pos constraints/platform-constrain filtered-platforms
                                                   old-pos (vec2/add old-pos (vec2/vec2 0 0.1)))



                        enemy-vel-x (Math/abs (vec2/get-x old-vel))
                        joy-acc (vec2/vec2 joy-dx 0)

                        new-vel (-> old-vel
                                    (vec2/add joy-acc)
                                    (vec2/scale 0.98)
                                    )

                        new-pos (-> old-pos
                                    (vec2/add new-vel))

                        passable-fn platforms/passable?

                        con-pos
                        (constraints/constrain-pos constraints/platform-constrain
                                                   (assoc-in filtered-platforms [0 :passable?] passable-fn)
                                                   old-pos new-pos)

                        old-vel (if (= :walking state) (vec2/sub con-pos old-pos)
                                    (-> (vec2/sub con-pos old-pos)
                                        (vec2/set-y 0)))
                        ]
                    (case facing
                      :left (s/set-scale! enemy -1 1)
                      :right (s/set-scale! enemy 1 1)
                      )
                    ;;(js/console.log "!" fallen-pos)

                    (recur state
                           (inc fnum)
                           old-vel
                           con-pos
                           (case (Math/sign joy-dx)
                             1 :left
                             0 facing
                             -1 :right
                             )
                           )))))

      )
    ))
