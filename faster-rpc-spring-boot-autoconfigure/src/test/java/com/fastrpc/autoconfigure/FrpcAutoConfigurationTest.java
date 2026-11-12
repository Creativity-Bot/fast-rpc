package com.fastrpc.autoconfigure;

import com.fastrpc.registry.Registry;
import com.fastrpc.server.impl.NettyServer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class FrpcAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FrpcAutoConfiguration.class));

    @Test
    void clientInfrastructureIsPresentWhenServerIsDisabled() {
        runner.withPropertyValues("fastrpc.server.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(Registry.class);
                    assertThat(context).hasSingleBean(FrpcClientBeanPostProcessor.class);
                    assertThat(context).hasSingleBean(FrpcServiceBeanPostProcessor.class);
                    assertThat(context).doesNotHaveBean(NettyServer.class);
                });
    }

    @Test
    void serverStartsAfterTheContextIsReady() {
        runner.withPropertyValues("fastrpc.server.enabled=true", "fastrpc.server.port=0")
                .run(context -> {
                    assertThat(context).hasSingleBean(NettyServer.class);
                    assertThat(context).hasSingleBean(FrpcServerLifecycle.class);
                    assertThat(context.getBean(FrpcServerLifecycle.class).isRunning()).isTrue();
                    assertThat(context.getBean(NettyServer.class).getPort()).isZero();
                });
    }
}
