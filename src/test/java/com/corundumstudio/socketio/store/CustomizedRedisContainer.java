package com.corundumstudio.socketio.store;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

import com.github.dockerjava.api.command.InspectContainerResponse;

/** Customized Redis container for testing purposes. */
public class CustomizedRedisContainer extends GenericContainer<CustomizedRedisContainer> {
  private static final Logger log = LoggerFactory.getLogger(CustomizedRedisContainer.class);
  public static final int REDIS_PORT = 6379;

  /**
   * Default constructor that initializes the Redis container with the official latest Redis image.
   */
  public CustomizedRedisContainer() {
    super("redis");
  }

  @Override
  protected void configure() {
    withExposedPorts(REDIS_PORT);
  }

  @Override
  protected void containerIsStarted(InspectContainerResponse containerInfo) {
    try {
      execInContainer("redis-server");
      ExecResult result = null;
      while (result == null || result.getExitCode() != 0) {
        TimeUnit.SECONDS.sleep(1);
        log.info("executing command to ensure redis is started");
        result = execInContainer("redis-cli", "ping");
        log.info("stdout: {}", result.getStdout());
        log.info("stderr: {}", result.getStderr());
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to start Redis container", e);
    }
  }

  @Override
  public void start() {
    super.start();
    log.info("Redis started at port: {}", getRedisPort());
  }

  @Override
  public void stop() {
    super.stop();
    log.info("Redis stopped");
  }

  public int getRedisPort() {
    return getMappedPort(REDIS_PORT);
  }
}
