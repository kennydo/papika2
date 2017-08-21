package net.hanekawa.papika.fromslack

import net.hanekawa.papika.common.getLogger
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.framework.recipes.leader.CancelLeadershipException
import org.apache.curator.framework.recipes.leader.LeaderSelector
import org.apache.curator.framework.recipes.leader.LeaderSelectorListener
import org.apache.curator.framework.state.ConnectionState
import org.apache.curator.retry.ExponentialBackoffRetry

class LeaderCandidate(val zookeeperConnect: String, val leadershipFlag: LeadershipFlag) {
    companion object {
        val LOG = getLogger(this::class.java)
        val leaderPath = "/fromSlackLeader"
    }

    private val retryPolicy = ExponentialBackoffRetry(1000, 5)
    private val curatorClient = CuratorFrameworkFactory.newClient(zookeeperConnect, retryPolicy)
    private val leaderSelector = LeaderSelector(curatorClient, leaderPath, object : LeaderSelectorListener {
        override fun stateChanged(client: CuratorFramework, newState: ConnectionState) {
            LOG.info("ZK client state changed to {}", newState)
            if (client.connectionStateErrorPolicy.isErrorState(newState)) {
                LOG.info("Relinquishing leadership")
                synchronized(leadershipFlag) {
                    leadershipFlag.isLeader = false
                }
                throw CancelLeadershipException()
            }
        }

        override fun takeLeadership(client: CuratorFramework) {
            LOG.info("Taking leadership!")
            synchronized(leadershipFlag) {
                leadershipFlag.isLeader = true
            }
            while (true) {
                Thread.sleep(10000)
            }
        }
    })

    fun run() {
        curatorClient.start()

        LOG.info("Announcing candidacy for leadership")
        leaderSelector.start()

        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                LOG.info("Stopping candidacy for leadership")
                try {
                    leaderSelector.close()
                } catch (e: Exception) {
                    LOG.error("Unable to close leader selector", e)
                }
            }
        })
    }
}