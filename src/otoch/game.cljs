(ns otoch.game

  (:require [infinitelives.utils.vec2 :as vec2]
            [infinitelives.utils.events :as e]
            [infinitelives.utils.sound :as sound]
            [infinitelives.utils.async :as async]
            [infinitelives.pixi.sprite :as s]
            [infinitelives.pixi.resources :as r]
            [infinitelives.pixi.canvas :as c]
            [infinitelives.pixi.texture :as t]
            [infinitelives.pixi.pixelfont :as pf]
            [otoch.consts :as consts]
            [otoch.enemy :as enemy]
            [otoch.map :as tm]
            [otoch.state :as state]
            [otoch.controls :as controls]
            [otoch.platforms :as platforms]
            [otoch.particle :as particle]
            [otoch.pickup :as pickup]
            [otoch.heart :as heart]
            [otoch.constraints :as constraints]
            [cljs.core.match :refer-macros [match]])

  (:require-macros [cljs.core.async.macros :refer [go]])
  )

;; when player finished he wobbles around
(def y-amp (/ 7 64))
(def y-freq (/ 7 200))
(def x-amp (/ 10 64))
(def x-freq (/ 11 200))


(defn sin [amp freq n]
  (* amp (Math/sin (* n freq))))

(defn set-player-heart [player heart-position x y]
  (js/console.log "pos:" (vec2/add (vec2/scale heart-position 64) (vec2/vec2 x y)))
  (s/set-pos! player heart-position))

(defn set-beams [beam-1 beam-2 beam-3 x y fnum]
  (s/set-pos! beam-1
              (-> tm/heart-position
                  (vec2/add (vec2/vec2 -0.4 0))
                  (vec2/scale 64)
                  (vec2/add (vec2/scale
                             (vec2/vec2 1 0)
                             (* (sin 2 0.1 fnum)
                                (sin 2 0.3 fnum)
                                (sin 2 0.037 fnum)
                                )))
                  (vec2/add (vec2/vec2 x -5000)))
              )
  (s/set-alpha! beam-1 0.5)
  (s/set-scale! beam-1 1 1)

  (s/set-pos! beam-2 (+ x (* 61.75 64) (sin 8 0.12 fnum)) (- y 5000))

  (s/set-pos! beam-2
              (-> tm/heart-position
                  (vec2/add (vec2/vec2 -0.25 0))
                  (vec2/scale 64)
                  (vec2/add (vec2/scale
                             (vec2/vec2 1 0)
                             (* (sin 8 0.1252 fnum)

                                )))
                  (vec2/add (vec2/vec2 x -5000)))
              )

  (s/set-alpha! beam-2 0.5)
  (s/set-scale! beam-2 0.5 1)

  (s/set-pos! beam-3 (int (+ x (* 61.875 64) (sin 8 0.08 fnum)))
              (int (- y 5000)))
  (s/set-pos! beam-3
              (-> tm/heart-position
                  (vec2/add (vec2/vec2 -0.15 0))
                  (vec2/scale 64)
                  (vec2/add (vec2/scale
                             (vec2/vec2 1 0)
                             (* (sin 8 0.0847 fnum)

                                )))
                  (vec2/add (vec2/vec2 x -5000)))
              )

  (s/set-alpha! beam-3 0.5)
  (s/set-scale! beam-3 0.25 1)
  )

(defn megalith-fn [start-frame n]
  (+ 3 start-frame n)
  )

(defn megalith-set-pos! [sprite pos]
  (let [
        spos (vec2/scale pos 64)
        [x y] spos]
    (s/set-pos! sprite
;                spos
                (int x) (int (+ 32 y))
                )))

;; work out if we are standing on a platform, and if
;; so, which one?  to do this, we simulate the
;; player falling every so slightly by a small
;; amount and by a larger amount. we then constrain
;; both these movements by the platform in
;; question. If the y value is the same between the
;; two end points, then we are standing on the
;; platform (even if platform is moving!)
(defn which-platform? [old-pos plats]
  (let [start old-pos
        num (count plats)
        end1 (vec2/add old-pos (vec2/vec2 0 0.1))
        end2 (vec2/add old-pos (vec2/vec2 0 0.3))]
    (loop [n 0]
      (let [{:keys [passable? platform-pos]} (nth plats n)]
        (if (= (vec2/get-y (constraints/platform-constrain passable? platform-pos start end1))
               (vec2/get-y (constraints/platform-constrain passable? platform-pos start end2)))
          n
          (when (< n (dec num))
            (recur (inc n))))))))


(defn make-text-display
  "create an updating number display with icon. Used for gold/rune etc.
  icon and text appeads at `y` with font `font` displaying string `s`.
  returns a channel. When you push a value down the channel, the
  display will update to that pushed value. When you close! the channel,
  the text disappears.
  "
  [icon y font s]
  (let [start-game-num (:game-num @state/state)]
    (async/go-while
     (= start-game-num (:game-num @state/state))
     (c/with-sprite :stats
       [icon (s/make-sprite icon :scale 1 :y (+ y -5))
        text (pf/make-text font (str s) :scale 2 :xhandle 0 :x 50 :y y)]
       (loop [s (:runes @state/state)]

         (<! (e/next-frame))
         (let [new-s (:runes @state/state)]
           (when (not= s new-s)
             ;; value changed
             (pf/change-text! text font (str new-s))
             (s/update-handle! text 0 0.5))
           (recur new-s)))))))

(defn make-foreground-map [bg-map-lines]
  (tm/mapv-mapv
   bg-map-lines
   (fn [c]
     (cond
       (#{:clover :grass-fg-1 :grass-fg-2 :grass-fg-3 :cactus-fg :reeds-fg :nubby-fg} c)
       c

       ;;(= c :ladder-top) :ladder-top-fg
       :default :space))))

(defn set-player
  "x y is scroll position.
  px, py is tile unit based position of player"
  [player x y px py]
  ;;(js/console.log "->" (+ x (* 16 4 px)) (+ y (* 16 4 py)))
  (swap! state/state assoc :pos (vec2/vec2 px py))
  (s/set-pos!
   player
   (+ x (* 16 4 px)) (+ y (* 16 4 py))))

(defn hollow
  "if vector is less than a certain size, make it zero"
  [v lim]
  (if (< (vec2/magnitude-squared v) lim)
    (vec2/zero)
    v))

(defn make-tree-mask []
  (doto (js/PIXI.Graphics.)
               (.beginFill 0xff0000)
               (.drawRect 0 0 256 256)
               (.endFill)))

(defn set-sprite-mask [sprite mask]
  (s/set-pos! mask -128 -128)
  (set! (.-mask sprite) mask)
  (.addChild sprite mask)
  )

(defn make-rune [container pos vel start-frame]
  (sound/play-sound :runethrow 0.2 false)
  (let [start-game-num (:game-num @state/state)
        mask (make-tree-mask)]
    (async/go-while
     (= start-game-num (:game-num @state/state))
     (c/with-sprite container
       [sprite (s/make-sprite :rune-1 :scale 0.5 :yhandle 1 :x (vec2/get-x pos) :y (vec2/get-y pos))]
       (let [final-pos (loop [n 0
                              p pos
                              v vel]
                         (megalith-set-pos! sprite p)
                         (<! (e/next-frame))
                         (if (< n 300)
                           ;; still alive
                           (do
                             (let [platform-state
                                   (-> platforms/platforms
                                       (platforms/prepare-platforms n)
                                       (platforms/filter-platforms (vec2/zero)))
                                   new-pos
                                   (constraints/constrain-pos
                                    constraints/rune-constrain
                                    (-> platforms/platforms
                                        (platforms/prepare-platforms (megalith-fn start-frame n))
                                        (platforms/filter-platforms p))
                                    p (vec2/add p v))
                                   new-vel (-> new-pos
                                               (vec2/sub p)
                                               (vec2/add consts/gravity)
                                               (vec2/scale 0.98))]
                               (recur (inc n)
                                      new-pos
                                      new-vel)))

                           ;; rune over. return final pos
                           p))]

         ;; megalith rise
         (let [tree (rand-nth [:tree-1 :tree-2 :tree-3 :tree-4
                               :tree-5 :tree-6 :tree-7 :tree-8])
               tree-height ({:tree-1 128
                             :tree-2 128
                             :tree-3 128
                             :tree-4 128
                             :tree-5 192
                             :tree-6 256
                             :tree-7 128
                             :tree-8 128
                             } tree)
               tile-height (/ tree-height 64)]
           (sound/play-sound :monolith 0.3 false)
           (s/set-texture! sprite tree)

           (set-sprite-mask sprite mask)

           (s/set-scale! sprite 1)
           (s/set-anchor-y! sprite 1)

           (let [[final-pos final-vel] (loop [n 300
                                              p final-pos
                                              v (vec2/zero)
                                              rise 1
                                              ]
                                         (megalith-set-pos! sprite (vec2/add p (vec2/vec2 0 (* tile-height rise))))
                                         (let [mask-y (+ 2 -256 (* rise (- tree-height)))]
                                           (s/set-pos! mask -128 mask-y))

                                         (<! (e/next-frame))

                                         (if (< n (+ 300 300))
                                           (let [platform-state
                                                 (-> platforms/platforms
                                                     (platforms/prepare-platforms n)
                                                     (platforms/filter-platforms (vec2/zero)))
                                                 new-pos
                                                 (constraints/constrain-pos
                                                  constraints/rune-constrain
                                                  (-> platforms/platforms
                                                      (platforms/prepare-platforms (megalith-fn start-frame n))
                                                      (platforms/filter-platforms p))
                                                  p (vec2/add p v))
                                                 new-vel (-> new-pos
                                                             (vec2/sub p)
                                                             (vec2/add consts/gravity)
                                                             (vec2/scale 0.98))]
                                             (recur (inc n)
                                                    new-pos
                                                    new-vel
                                                    (- rise (/ 1 300))))

                                           [p v]))]

             (swap! state/state update :trees inc)

             ;; megalith runs forever
             (loop [
                    n (+ 300 300)
                    p final-pos
                    v final-vel
                    ]
               (megalith-set-pos! sprite p)
               (<! (e/next-frame))

               (let [platform-state
                     (-> platforms/platforms
                         (platforms/prepare-platforms n)
                         (platforms/filter-platforms (vec2/zero)))
                     new-pos
                     (constraints/constrain-pos
                      constraints/rune-constrain
                      (-> platforms/platforms
                          (platforms/prepare-platforms (megalith-fn start-frame n))
                          (platforms/filter-platforms p))
                      p (vec2/add p v))
                     new-vel (-> new-pos
                                 (vec2/sub p)
                                 (vec2/add consts/gravity)
                                 (vec2/scale 0.98))]
                 (recur (inc n)
                        new-pos
                        new-vel
                        ))))))))))

(defn run [canvas tile-set]
  (go
    (state/reset-state!)

    ;; make the tile texture lookup
    (let [
          stand (t/sub-texture (r/get-texture :tiles :nearest) [0 (* 4 96)] [64 64])
          walk (t/sub-texture (r/get-texture :tiles :nearest) [(* 4 16) (* 4 96)] [64 64])
          tilemap-order-lookup (tm/make-tiles-struct tile-set tm/tile-map)

          rune-display (make-text-display :rune-1 0 :pixel 0)

          heart-position (vec2/vec2 62 -5)

          cell-width 32
          cell-height 16
          partitioned-map (tm/partition-tilemap tm/tile-map cell-width cell-height)

          partitioned-map
          (tm/make-container-chunks tile-set partitioned-map)

          ]

      ;; all tilemap chunks invisible
      ;; (loop [[chunk & r] (vals partitioned-map)]
      ;;   (s/set-visible! (:sprites chunk) false)
      ;;   (when r (recur r)))

      ;; ;; once centered on us visible
      ;; (s/set-visible! (-> partitioned-map (get [50 0]) :sprites) true)

      #_(c/with-sprite canvas :tilemap
          [tilemap  (s/make-container
                     :children (tm/make-tiles tile-set tile-map)
                     :xhandle 0 :yhandle 0
                     :scale 1
                     :particle true
                     :particle-opts #{:alpha})]


          )

      ;; create sprite and tile map batches
      (c/with-sprite canvas :tilemap
        [
         background-2 (js/PIXI.TilingSprite.
                       (t/sub-texture
                        (r/get-texture :background-2 :nearest)
                        [0 0] [1024 1024])
                       10000 10000)


         background (js/PIXI.TilingSprite.
                     (t/sub-texture
                      (r/get-texture :background-1 :nearest)
                      [0 0] [1024 1024])
                     10000 10000)

         ;; {:size [64 64] :pos [(* 15 64) (* 6 64)]}
         beam-1 (js/PIXI.TilingSprite.
                 (t/sub-texture
                  (r/get-texture :tiles :nearest)
                  [(+ 10 (* 15 64)) (* 6 64)]
                  [44 1]
                  )
                 44 10000)

         beam-2 (js/PIXI.TilingSprite.
                 (t/sub-texture
                  (r/get-texture :tiles :nearest)
                  [(+ 10 (* 15 64)) (* 8 64)]
                  [44 1]
                  )
                 44 10000)

         beam-3 (js/PIXI.TilingSprite.
                 (t/sub-texture
                  (r/get-texture :tiles :nearest)
                  [(+ 10 (* 15 64)) (* 10 64)]
                  [44 1]
                  )
                 44 10000)


         runes (s/make-container :scale 1 :particle false)

         tilemap (s/make-container
                  :children (map :sprites (vals partitioned-map))
                  ;;:xhandle 0 :yhandle 0
                  ;;:x (* 64 50)
                  ;;:y 0
                  :scale 1
                  ;;:particle true
                  ;;:particle-opts #{:alpha}
                  )
         platform (s/make-container
                   :children (tm/make-tiles tile-set platforms/platform-map)
                   ;; :xhandle 0 :yhandle 0
                   :scale 1
                   ;;:particle true
                   )

         platform-cavern-1 (s/make-container
                            :children (tm/make-tiles tile-set platforms/platform-map)
                            ;; :xhandle 0 :yhandle 0
                            :scale 1
                            ;;:particle true
                            )

         platform-cavern-2 (s/make-container
                            :children (tm/make-tiles tile-set platforms/platform-map)
                            ;; :xhandle 0 :yhandle 0
                            :scale 1
                            ;;:particle true
                            )

         platform-cavern-3 (s/make-container
                            :children (tm/make-tiles tile-set platforms/platform-map)
                            ;; :xhandle 0 :yhandle 0
                            :scale 1
                            ;;:particle true
                            )

         platform-cavern-4 (s/make-container
                            :children (tm/make-tiles tile-set platforms/platform-map)
                            ;; :xhandle 0 :yhandle 0
                            :scale 1
                            ;;:particle true
                            )

         behind-player (s/make-container)

         player (s/make-sprite stand :scale 1)
         foreground (s/make-container
                     :children (tm/make-tiles tile-set (into [] (make-foreground-map tm/tile-map)))
                     ;;:xhandle 0 :yhandle 0
                     :scale 1
                     ;;:particle true
                     )

         enemies (s/make-container)


         ]

        ;; disable enemies by setting this false
        (when true

          (doseq [[enemy-type locations] tm/enemies]
            (doseq [[n [x y]] (map-indexed vector locations)]
              (enemy/spawn enemies (vec2/vec2 x y) enemy-type n))))

        (heart/spawn behind-player)

        (doseq [[x y] tm/rune-locations]
          (pickup/spawn behind-player :rune 0 (vec2/vec2 x y)))

        (s/set-scale! background 1)
        (s/set-scale! background-2 1)
        (loop [
               state :walking
               fnum 0
               old-vel (vec2/vec2 0 0)
               ppos tm/start-position
               jump-pressed 0
               last-x-pressed? (e/is-pressed? :x)
               facing :left
               ]

          (let [
                original-vel old-vel
                old-pos ppos
                pos (-> ppos
                        (vec2/scale (* -2 32)))
                x (vec2/get-x pos) ;; (+ -2000 (* 1000 (Math/sin theta)))
                y (vec2/get-y pos) ;; (+ -1000 (* 500 (Math/cos theta)))

                px (vec2/get-x ppos)
                py (vec2/get-y ppos)
                pix (int px)
                piy (int py)
                dx (- px pix)
                dy (- py piy)

                joy (controls/get-joy-vec)

                platforms-this-frame (platforms/prepare-platforms platforms/platforms fnum)

                ;; platform subset for player
                filtered-platforms (platforms/filter-platforms
                                    platforms-this-frame old-pos)
                ]

            (s/set-texture! player
                            (if (> (Math/abs (vec2/get-x joy)) 0.02)
                              (if (zero? (mod (int (/ fnum 10)) 2)) stand walk)
                              stand))

            ;; player still playing
            (set-player player x y px py)

            (s/set-pos! runes x y)

            ;;(js/console.log "===>" (str platforms-this-frame))

            ;; set tilemap positions
            (doall
             (map
              (fn [{:keys [name old-platform-pos]} obj]
                (let [pos (-> old-platform-pos
                              (vec2/scale (* 4 16))
                              (vec2/add (vec2/vec2 x y)))
                      [x y] pos]
                  (s/set-pos! obj (int x) (int y))))
              platforms-this-frame
              [tilemap
               platform
               platform-cavern-1
               platform-cavern-2
               platform-cavern-3
               platform-cavern-4
               ]))

            ;; all tilemap chunks invisible
            (loop [[chunk & r] (vals partitioned-map)]
              (s/set-visible! (:sprites chunk) false)
              (when r (recur r)))

            ;; once centered on us visible
            (let [xc (* cell-width (int (/ px cell-width)))
                  yc (* cell-height (int (/ py cell-height)))
                  xrem (mod px cell-width)
                  yrem (mod py cell-height)
                  left? (< xrem (/ cell-width 2))
                  top? (< yrem (/ cell-height 2))
                  right? (not left?)
                  bottom? (not top?)
                  ]
              ;;(js/console.log "=" left? top?)
              (s/set-visible! (-> partitioned-map (get [xc yc]) :sprites) true)
              (if top?
                (some-> partitioned-map (get [xc (- yc cell-height)]) :sprites (s/set-visible! true))
                (some-> partitioned-map (get [xc (+ yc cell-height)]) :sprites (s/set-visible! true)))

              (if left?
                (some-> partitioned-map (get [(- xc cell-width) yc]) :sprites (s/set-visible! true))
                (some-> partitioned-map (get [(+ xc cell-width) yc]) :sprites (s/set-visible! true)))

              (cond
                (and left? top?)
                (some-> partitioned-map (get [(- xc cell-width) (- yc cell-height)]) :sprites (s/set-visible! true))

                (and left? bottom?)
                (some-> partitioned-map (get [(- xc cell-width) (+ yc cell-height)]) :sprites (s/set-visible! true))

                (and right? top?)
                (some-> partitioned-map (get [(+ xc cell-width) (- yc cell-height)]) :sprites (s/set-visible! true))

                (and right? bottom?)
                (some-> partitioned-map (get [(+ xc cell-width) (+ yc cell-height)]) :sprites (s/set-visible! true))))


            (s/set-pos! foreground (int x) (int y))

            (set-beams beam-1 beam-2 beam-3 x y fnum)

            (s/set-pos! background
                        (+ -5000 (mod (int (* x 0.90)) 1024))
                        (+ -5000 (mod (int (* y 0.90)) 1024)))

            (s/set-pos! background-2
                        (+ -5000 (mod (int (* x 0.80)) 1024))
                        (+ -5000 (mod (int (* y 0.80)) 1024)))

            ;; save level pos so other go blocks can access
            ;; (swap! state/state assoc :pos (vec2/vec2 x y))

            (s/set-pos! enemies (int x) (int y))
            (s/set-pos! behind-player (int x) (int y))

            (<! (e/next-frame))
                                        ;(log dy minus-v-edge)
            (let [
                  square-on (tm/get-tile-at tm/tile-map pix piy)
                  square-below (tm/get-tile-at tm/tile-map pix (inc piy))
                  square-standing-on (tm/get-tile-at tm/tile-map pix
                                                     (int (+ 0.3 py)))

                  on-ladder-transition? (or (tm/all-ladders square-on)
                                            (tm/all-ladders square-below))

                  on-ladder? (tm/all-ladders square-standing-on)

                  ladder-up? (tm/all-ladders square-standing-on)
                  ladder-down? (tm/all-ladders square-below)

                  plat (which-platform? old-pos filtered-platforms)

                  ;;_ (js/console.log "PLAT:" plat)

                  ;; move oldpos by platform movement
                  old-pos (if plat
                            (-> filtered-platforms
                                (nth plat)
                                :platform-delta
                                (vec2/add old-pos))
                            old-pos)

                  ;; simulate a little vertical move down to see if we are
                  ;; standing on solid ground (or a platform)
                  fallen-pos
                  (constraints/constrain-pos constraints/platform-constrain filtered-platforms
                                             old-pos (vec2/add old-pos (vec2/vec2 0 0.1)))

                  ;; TODO: this is dodgy. We need to test with each platform.
                  ;; if we are standing on a platform, we are "bound to it"
                  ;; and we move where it does!
                  standing-on-ground? (> 0.06 (Math/abs (- (vec2/get-y fallen-pos) (vec2/get-y old-pos))))

                  jump-pressed (cond
                                 (and (controls/jump-button-pressed?) (zero? jump-pressed)
                                      (or standing-on-ground? on-ladder?))
                                 ;; cant jump off ladder! if you can, problem... when jumping off lader, state stays climbing causing no accel for the jump
                                 (inc jump-pressed)

                                 (and (controls/jump-button-pressed?) (pos? jump-pressed))
                                 (inc jump-pressed)

                                 :default
                                 0)

                  jump-force (if (and (<= 1 jump-pressed consts/jump-frames)
                                      (controls/jump-button-pressed?))
                               (vec2/vec2 0 (- (if (= 1 jump-pressed)
                                                 consts/jump-accel-1
                                                 consts/jump-accel-2+)))
                               (vec2/zero))

                  joy-dy (-> joy
                             (hollow 0.2)
                             (vec2/get-y))

                  joy-dy (cond
                           (and (not ladder-up?) (neg? joy-dy))
                           0

                           (and (not ladder-down?) (not ladder-up?) (pos? joy-dy))
                           0

                           :default joy-dy)

                  joy-dx (-> joy
                             (hollow 0.2)
                             (vec2/scale 0.005)
                             (vec2/get-x))

                  state-ladder? (and on-ladder-transition?
                                     (not (zero? joy-dy)))

                  joy-acc (if (= :climbing state)
                            (vec2/vec2 joy-dx (* 0.1 joy-dy))
                            (vec2/vec2 joy-dx 0))


                  player-vel-x (Math/abs (vec2/get-x old-vel))

                  ;; when moving quickly left and right, and the
                  ;; joystick is centered or reversed, this breaking
                  ;; horitontal force is applied
                  player-brake
                  (match [(Math/sign (vec2/get-x old-vel))
                          (Math/sign (vec2/get-x (hollow joy 0.5)))]
                         [1 0] (vec2/vec2 -1 0)
                         [1 -1] (vec2/vec2 -1 0)
                         [-1 0] (vec2/vec2 1 0)
                         [-1 1] (vec2/vec2 1 0)
                         [_ _] (vec2/zero))


                  next-state (if state-ladder?
                               :climbing
                               (if on-ladder? state :walking))

                  passable-fn (if (= :walking next-state)
                                platforms/walkable?
                                platforms/passable?)

                  new-vel (-> old-vel
                              ;; zero any y vel in the last frames vel if we are climbing
                              (vec2/set-y (if (= state :climbing) 0 (vec2/get-y old-vel)))
                              ;; no gravity on you when you are on the stairs
                              (vec2/add (if (= state :climbing) (vec2/zero) consts/gravity))

                              (vec2/add jump-force)
                              (vec2/add joy-acc)
                              (vec2/add (vec2/scale player-brake (/ player-vel-x 3)))
                              (vec2/scale 0.98)
                              )

                  new-pos (-> old-pos
                              (vec2/add new-vel))

                  con-pos
                  (constraints/constrain-pos constraints/platform-constrain
                                             (assoc-in filtered-platforms [0 :passable?] passable-fn)
                                             old-pos new-pos)

                  old-vel (if (= :walking state) (vec2/sub con-pos old-pos)
                              (-> (vec2/sub con-pos old-pos)
                                  (vec2/set-y 0)))

                  ]
              (when (and (pos? (:runes @state/state))
                         (not last-x-pressed?)
                         (controls/throw-rune-pressed?))
                (make-rune
                 runes ppos old-vel fnum)
                (swap! state/state update :runes dec))

              (when (and (controls/jump-button-pressed?) (= 1 jump-pressed) standing-on-ground?)
                (sound/play-sound :jump 0.3 false))

              (when (> (vec2/magnitude-squared (vec2/sub original-vel old-vel)) 0.01)
                (sound/play-sound :thud 0.06 false)
                )

              ;; hold down R U N E to crank up your rune count
              (when (and (e/is-pressed? :r)
                         (e/is-pressed? :u)
                         (e/is-pressed? :n)
                         (e/is-pressed? :e)

                         )
                (swap! state/state update :runes inc))

              (case facing
                :left (s/set-scale! player -1 1)
                :right (s/set-scale! player 1 1)
                )

              ;; have we collided with any enemies?
              (let [die? (enemy/collided? con-pos)]
                ;; (js/console.log "die?" die?)
                (if-not (or die?

                            ;; hold down D I E to die
                            (and (e/is-pressed? :d)
                                 (e/is-pressed? :i)
                                 (e/is-pressed? :e))

                            ;; have we hit a deathtile?
                            (#{:death-tile-upper-1
                               :death-tile-upper-2
                               :death-tile-lower-1
                               :death-tile-lower-2}
                             square-on
                             )
                            )
                  ;; still alive
                  (do

                    (if (-> @state/state :touched-heart?)
                      ;; finished!
                      (do (s/set-alpha! beam-1 0)
                          (s/set-alpha! beam-2 0)
                          (s/set-alpha! beam-3 0)

                          (loop [fnum fnum]

                            (let [[x y] [0 0]]
                              (s/set-pos! player
                                          (vec2/scale
                                           (vec2/vec2
                                            (+ x (sin x-amp x-freq fnum))
                                            (+ y (sin y-amp y-freq fnum))
                                            )
                                           64))
                              #_ (s/set-scale! heart (+ size-off (sin size-amp size-freq n))))

                            (<! (e/next-frame))

                            (recur (inc fnum))

                            ))

                      ;; still playing
                      (recur
                       next-state
                       (inc fnum)
                       old-vel
                       con-pos
                       jump-pressed
                       (controls/throw-rune-pressed?)
                       (case (Math/sign joy-dx)
                         -1 :left
                         0 facing
                         1 :right
                         ))))

                  ;; you get hit by enemy
                  ;; dead
                  (do
                    (sound/play-sound :death 0.3 false)

                    ;; particles
                    (doseq [n (range 16)]
                      (let [vel (vec2/add
                                 (vec2/scale (vec2/add (vec2/random)
                                                       (vec2/vec2 0 -0.3)
                                                       ) 0.1)
                                 (vec2/scale old-vel 0.33)
                                 )]
                        ;;(js/console.log (str vel))
                        (particle/spawn enemies
                                        (rand-nth [:blood-1 :blood-2 :blood-3 :blood-4
                                                   :blood-5 :blood-6 :blood-7 :blood-8
                                                   :blood-9 :blood-10 :blood-11 :blood-12
                                                   :blood-13 :blood-14 :blood-15 :blood-16
                                                   ])
                                        (inc fnum)
                                        (vec2/scale con-pos 1)
                                        vel
                                        (/ (- (rand) 0.5) 3)
                                        )))

                    ;; dead
                    (s/set-alpha! player 0)
                    (loop [n 300]
                      (<! (e/next-frame))
                      (when (pos? n)
                        (recur (dec n))))

                    ;; kill goroutines
                    (swap! state/state update :game-num inc)
                    (<! (e/next-frame))))))))))))