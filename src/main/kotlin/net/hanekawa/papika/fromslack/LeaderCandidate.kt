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


class LeaderCandidate(val zookeeperConnect: String) {
    companion object {
        val LOG = getLogger(this::class.java)
        val leaderPath = "/fromSlackLeader"
    }

    var eligibleForLeadership = true

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
                    throw CancelLeadershipException()
                }

                if (eligibleForLeadership) {
                    leaderSelector.requeue()
                }
            }

            override fun takeLeadership(client: CuratorFramework) {
                if(!eligibleForLeadership){
                    LOG.warn("Ineligible for leadership, but still tried to take leadership!")
                    return
                }
                LOG.info("Taking leadership!")
                while (eligibleForLeadership) {
                    Thread.sleep(5_000)
                }
            }
        })

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
                    curatorClient.close()
                    leaderSelector.close()
                } catch (e: Exception) {
                    LOG.error("Unable to close leader selector", e)
                }
            }
        })
    }

    fun abandonLeadership() {
        LOG.info("Abandoning leadership")
        synchronized(this) {
            eligibleForLeadership = false
            leaderSelector.interruptLeadership()
        }
    }

    fun hasLeadership(): Boolean {
        return leaderSelector.hasLeadership()
    }
}