package io.ast.jneurocarto.app;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;

import io.ast.jneurocarto.core.cli.CartoConfig;
import io.ast.jneurocarto.core.config.CartoUserConfig;
import io.ast.jneurocarto.core.config.Repository;
import picocli.CommandLine;

@SpringBootApplication
@Theme("default")
public class Application implements CommandLineRunner, ExitCodeGenerator, AppShellConfigurator {

    private CommandLine.IFactory factory;
    private final CartoConfig config;
    private int exitCode;

    public Application(CommandLine.IFactory factory) {
        this.factory = factory;
        this.config = new CartoConfig();
        // overwrite
        config.debug = !System.getProperty("io.ast.jneurocarto.app.debug", "").isEmpty();
    }

    @Bean
    public CartoConfig getCartoConfig() {
        return config;
    }

    @Bean
    public Repository getCartoRepository() {
        return new Repository(config);
    }

    @Bean
    public CartoUserConfig getCartoUserConfig(Repository repository) {
        return repository.getUserConfig();
    }

    @Override
    public void run(String... args) throws Exception {
        exitCode = new CommandLine(config, factory).execute(args);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
