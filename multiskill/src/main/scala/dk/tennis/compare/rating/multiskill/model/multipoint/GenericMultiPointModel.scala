package dk.tennis.compare.rating.multiskill.model.multipoint

import dk.tennis.compare.rating.multiskill.domain.PlayerSkill
import scala.annotation.tailrec
import dk.bayes.math.gaussian.Gaussian
import scala.math._

case class GenericMultiPointModel(p1PerfVariance: Double,p2PerfVariance: Double) extends MultiPointModel {

  def skillMarginals(player1Skill: PlayerSkill, player2Skill: PlayerSkill, pointsWon: Int, allPoints: Int): Tuple2[PlayerSkill, PlayerSkill] = {

    val p1SkillFactor = Gaussian(player1Skill.mean, player1Skill.variance)
    val p2SkillFactor = Gaussian(player2Skill.mean, player2Skill.variance)

    val threshold = 1e-5

    val factorGraph = PointsFactorGraph(p1SkillFactor, p2SkillFactor, p1PerfVariance,p2PerfVariance, pointsWon, allPoints)

    @tailrec
    def calibrate(p1Marginal: Gaussian, p2Marginal: Gaussian): Tuple2[Gaussian, Gaussian] = {
      factorGraph.sendMsgs()

      val newP1Marginal = factorGraph.getP1Marginal()
      val newP2Marginal = factorGraph.getP2Marginal()

      if (equals(newP1Marginal, p1Marginal, threshold) && equals(newP2Marginal, p2Marginal, threshold))
        (newP1Marginal, newP2Marginal)
      else calibrate(newP1Marginal, newP2Marginal)
    }

    val (p1Marginal, p2Marginal) = calibrate(p1SkillFactor, p2SkillFactor)
    (PlayerSkill(p1Marginal.m, p1Marginal.v), PlayerSkill(p2Marginal.m, p2Marginal.v))

  }

  private def equals(gaussian1: Gaussian, gaussian2: Gaussian, threshold: Double): Boolean =
    abs(gaussian1.m - gaussian1.m) < threshold && (abs(gaussian1.v - gaussian2.v) < threshold)
}