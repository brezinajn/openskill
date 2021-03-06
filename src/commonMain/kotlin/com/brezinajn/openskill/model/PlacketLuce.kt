package com.brezinajn.openskill.model

import com.brezinajn.openskill.teamRating
import com.brezinajn.openskill.utilA
import com.brezinajn.openskill.utilC
import com.brezinajn.openskill.utilSumQ
import kotlin.math.exp
import kotlin.math.sqrt


interface PlacketLuce<TEAM, PLAYER> : Model<TEAM, PLAYER> {
    override operator fun invoke(game: List<TEAM>): List<TEAM> {
        val teamRatings = teamRating(game)
        val c = utilC(teamRatings)
        val sumQ = utilSumQ(teamRatings, c)
        val a = utilA(teamRatings)

        return teamRatings.map { (iMu, iSigmaSq, iTeam, iRank) ->
            val (omegaSet, deltaSet) = teamRatings.filter { (_, _, _, qRank) -> qRank <= iRank }
                .map { (_, _, _, qRank) ->
                    val quotient = exp(iMu / c) / sumQ[qRank]
                    val mu = if (qRank == iRank) 1 - quotient / a[qRank] else -quotient / a[qRank]
                    val sigma = (quotient * (1 - quotient)) / a[qRank]
                    Pair(mu, sigma)
                }.transpose()

            val gamma = sqrt(iSigmaSq) / c
            val iOmega = omegaSet.sum() * iSigmaSq / c
            val iDelta = gamma * deltaSet.sum() * iSigmaSq / c / c

            iTeam.setPlayers(
                iSigmaSq = iSigmaSq,
                iOmega = iOmega,
                iDelta = iDelta
            )
        }
    }

    companion object {
        operator fun <TEAM, PLAYER> invoke(
            sigmaSetter: (PLAYER, Double) -> PLAYER,
            sigmaGetter: (PLAYER) -> Double,
            muSetter: (PLAYER, Double) -> PLAYER,
            muGetter: (PLAYER) -> Double,
            playersGetter: (TEAM) -> List<PLAYER>,
            playersSetter: (TEAM, List<PLAYER>) -> TEAM
        ) = object : PlacketLuce<TEAM, PLAYER> {
            override val sigmaSetter = sigmaSetter
            override val muSetter = muSetter
            override val playersSetter = playersSetter

            override val TEAM.players: List<PLAYER>
                get() = playersGetter(this)
            override val PLAYER.mu: Double
                get() = muGetter(this)
            override val PLAYER.sigma: Double
                get() = sigmaGetter(this)
        }
    }
}

private fun <A, B> List<Pair<A, B>>.transpose(): Pair<List<A>, List<B>> = fold(
    mutableListOf<A>() to mutableListOf<B>()
) { acc, it ->
    acc.first.add(it.first)
    acc.second.add(it.second)

    acc
}