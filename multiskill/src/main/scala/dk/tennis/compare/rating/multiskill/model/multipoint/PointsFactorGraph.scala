package dk.tennis.compare.rating.multiskill.model.multipoint

import dk.bayes.math.gaussian.Gaussian

case class PointsFactorGraph(p1SkillFactor: Gaussian, p2SkillFactor: Gaussian, p1PerfVariance: Double,p2PerfVariance: Double, pointsWon: Int, allPoints: Int) {

  private var p1WonPerfToSkillMsg = Gaussian(0, Double.PositiveInfinity)
  private var p1LostPerfToSkillMsg = Gaussian(0, Double.PositiveInfinity)

  private var p2WonPerfToSkillMsg = Gaussian(0, Double.PositiveInfinity)
  private var p2LostPerfToSkillMsg = Gaussian(0, Double.PositiveInfinity)

  private var p1Marginal = p1SkillFactor
  private var p2Marginal = p2SkillFactor

  def getP1Marginal(): Gaussian = p1Marginal
  def getP2Marginal(): Gaussian = p2Marginal

  def sendMsgs() {

    val (newP1WonPerfToSkillMsg, newP2LostPerfToSkillMsg) = calcPerfToSkillMsgs(p1Marginal / p1WonPerfToSkillMsg, p2Marginal / p2LostPerfToSkillMsg, true)
    p1WonPerfToSkillMsg = newP1WonPerfToSkillMsg
    p2LostPerfToSkillMsg = newP2LostPerfToSkillMsg

    val (newP2WonPerfToSkillMsg, newP1LostPerfToSkillMsg) = calcPerfToSkillMsgs(p2Marginal / p2WonPerfToSkillMsg, p1Marginal / p1LostPerfToSkillMsg, true)
    p2WonPerfToSkillMsg = newP2WonPerfToSkillMsg
    p1LostPerfToSkillMsg = newP1LostPerfToSkillMsg

    p1Marginal = p1SkillFactor * power(p1WonPerfToSkillMsg, pointsWon) * power(p1LostPerfToSkillMsg, allPoints - pointsWon)
    p2Marginal = p2SkillFactor * power(p2WonPerfToSkillMsg, allPoints - pointsWon) * power(p2LostPerfToSkillMsg, pointsWon)

  }

  /**
   * @return [p1PerfToSkillMsg,p2PerfToSkillMsg]
   */
  private def calcPerfToSkillMsgs(p1SkillToPerfMsg: Gaussian, p2SkillToPerfMsg: Gaussian, p1Wins: Boolean): Tuple2[Gaussian, Gaussian] = {

    val p1_perf_to_diff = Gaussian(p1SkillToPerfMsg.m, p1SkillToPerfMsg.v + p1PerfVariance)
    val p2_perf_to_diff = Gaussian(p2SkillToPerfMsg.m, p2SkillToPerfMsg.v + p2PerfVariance)

    val diff_to_outcome = p1_perf_to_diff - p2_perf_to_diff
    val outcome_to_diff = (diff_to_outcome.truncate(0, p1Wins)) / diff_to_outcome

    val diff_to_p1_perf = outcome_to_diff + p2_perf_to_diff
    val diff_to_p2_perf = p1_perf_to_diff - outcome_to_diff

    val p1_perf_to_skill = Gaussian(diff_to_p1_perf.m, diff_to_p1_perf.v + p1PerfVariance)
    val p2_perf_to_skill = Gaussian(diff_to_p2_perf.m, diff_to_p2_perf.v + p2PerfVariance)

    (p1_perf_to_skill, p2_perf_to_skill)
  }

  private def power(gaussian: Gaussian, x: Int): Gaussian = {
    x match {
      case 0 => Gaussian(0, Double.PositiveInfinity)
      case _ => {
        var product = gaussian
        var i = 1
        while (i < x) {
          product = product * gaussian
          i += 1
        }
        product
      }
    }
  }

}