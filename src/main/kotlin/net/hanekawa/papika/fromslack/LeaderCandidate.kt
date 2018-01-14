package net.hanekawa.papika.fromslack

import net.hanekawa.papika.common.getLogger
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.framework.recipes.leader.CancelLeadershipException
import org.apache.curator.framework.recipes.leader.LeaderSelector
import org.apache.curator.framework.recipes.leader.LeaderSelectorListener
import org.apache.curator.framework.state.ConnectionState
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.zookeeper.client.ConnectStringParser

class LeaderCandidate(val zookeeperConnect: String, val leadershipFlag: LeadershipFlag) {
    companion object {
        val LOG = getLogger(this::class.java)
        val leaderPath = "/fromSlackLeader"
    }

    private val zkConnectStringParser = ConnectStringParser(zookeeperConnect)
    private val retryPolicy = ExponentialBackoffRetry(1000, 5)

    private var curatorClient = createCuratorClient()
    private var leaderSelector = createLeaderSelector(curatorClient)

    fun createCuratorClient(): CuratorFramework {
        return CuratorFrameworkFactory.newClient(zookeeperConnect, retryPolicy)
    }

    private fun createLeaderSelector(curatorClient: CuratorFramework): LeaderSelector {
        val leaderSelector = LeaderSelector(curatorClient, leaderPath, object : LeaderSelectorListener {
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

        // We want the leader selector to try again if there is any error while being leader
        leaderSelector.autoRequeue()

        return leaderSelector
    }

    fun run() {
        curatorClient.start()

        val chrootPath = zkConnectStringParser.chrootPath

        // The existence check operates within the chroot, so we look for '/', not the configured chroot
        if (chrootPath != null && curatorClient.checkExists().forPath("/") == null) {
            LOG.error("Zookeeper chroot path does not exist, please ensure it exists: {}", chrootPath)
            throw RuntimeException("Invalid chroot path")
        }

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