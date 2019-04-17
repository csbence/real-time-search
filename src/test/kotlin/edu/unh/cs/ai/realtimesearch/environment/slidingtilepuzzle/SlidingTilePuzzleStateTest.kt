//package edu.unh.cs.searkt.environment.slidingtilepuzzle
//
//import edu.unh.cs.searkt.environment.location.Location
//import org.junit.Test
//import kotlin.test.assertTrue
//
//class SlidingTilePuzzleStateTest {
//
//    @Test
//    fun testHashCode() {
//        val tiles1 = tiles(3) {
//            row (1, 2, 0)
//            row (3, 4, 5)
//            row (6, 7, 8)
//        }
//
//        val tiles2 = tiles(3) {
//            row (1, 2, 0)
//            row (3, 4, 5)
//            row (6, 7, 8)
//        }
//
//        val slidingTilePuzzle = SlidingTilePuzzle(3, 0)
//
//        val location1 = Location(2, 0)
//        val location2 = Location(2, 0)
//
//        val state1 = SlidingTilePuzzle4State(location1.x, location1.y, tiles1, slidingTilePuzzle.heuristic(tiles1))
//        val state2 = SlidingTilePuzzle4State(location2.x, location2.y, tiles2, slidingTilePuzzle.heuristic(tiles2))
//
//        assertTrue(location1 == location2)
//        assertTrue(location1.hashCode() == location2.hashCode())
//
//        assertTrue(tiles1 == tiles2)
//        assertTrue(tiles1.hashCode() == tiles2.hashCode())
//
//        assertTrue(state1 == state2)
//        assertTrue(state1.hashCode() == state2.hashCode())
//    }
//}