/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import static org.dragonskulle.network.testing.NetworkedTestContext.TIMEOUT;

import org.dragonskulle.game.lobby.Lobby;
import org.dragonskulle.network.components.ClientNetworkManager.ConnectionState;
import org.dragonskulle.network.testing.NetworkedSceneContext;
import org.dragonskulle.network.testing.NetworkedTestContext;
import org.junit.Test;

/** Unit test for simple App. */
public class AppTest {

    /** Simple latency selector that allows tester to pick latency for a client. */
    public static interface ILatencySelector {
        /**
         * Pick latency for a client.
         *
         * @param clientId input client ID (in the test context client list).
         * @return selected latency.
         */
        float selectLatency(int clientId);
    }

    /**
     * Build a game testing context.
     *
     * <p>This context sets up the scene into the game scene for a number of clients given.
     *
     * @param numClients number of clients.
     * @param app instance of {@link App}.
     * @return ready to use networked test context.
     */
    public static NetworkedTestContext buildTestContext(
            int numClients, App app, ILatencySelector selector) {

        NetworkedTestContext ctx =
                new NetworkedTestContext(
                        numClients,
                        app.createTemplateManager(),
                        App::createMainScene,
                        null,
                        Lobby::onClientLoaded,
                        Lobby::onGameStarted,
                        null // intentionally do not spawn human player
                        );

        int[] id = {0};

        // Pick client latency, and synchronize the server with the clients being connected
        ctx.getClients().stream()
                .forEach(
                        c -> {
                            float latency = selector != null ? selector.selectLatency(id[0]++) : 0;

                            c.then(
                                    (__) ->
                                            c.getManager()
                                                    .getClientManager()
                                                    .getClient()
                                                    .setSimLatency(latency));

                            NetworkedSceneContext pctx =
                                    c.awaitTimeout(
                                            TIMEOUT,
                                            (__) ->
                                                    ctx.getClientManager().getConnectionState()
                                                            == ConnectionState.CONNECTED);

                            ctx.getServer().syncWith(pctx);
                        });

        ctx.getServer().then((__) -> ctx.getServer().getManager().getServerManager().start(false));

        // Await and sync, to make sure that the conditions do not run after disconnect
        ctx.getClients().stream()
                .forEach(
                        c ->
                                ctx.getServer()
                                        .syncWith(
                                                c.awaitTimeout(
                                                        TIMEOUT,
                                                        (__) ->
                                                                ctx.getClientManager()
                                                                                .getConnectionState()
                                                                        == ConnectionState
                                                                                .JOINED_GAME)));

        return ctx;
    }

    /**
     * Build a game testing context.
     *
     * <p>This context sets up the scene into the game scene for one client given.
     *
     * @param app instance of {@link App}.
     * @return ready to use networked test context.
     */
    public static NetworkedTestContext buildTestContext(App app) {
        return buildTestContext(1, app, null);
    }

    /** Test if one client can connect to the game. */
    @Test
    public void testGameConnect() {
        buildTestContext(new App()).execute();
    }

    /** Test if 8 clients can connect to the game. */
    @Test
    public void testMultipleGameConnect() {
        buildTestContext(8, new App(), null).execute();
    }
}
