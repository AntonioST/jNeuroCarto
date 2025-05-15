package io.ast.jneurocarto.app;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;

import io.ast.jneurocarto.app.cli.CartoConfig;
import picocli.CommandLine;

@SpringBootApplication
@Theme("default")
public class Application implements CommandLineRunner, ExitCodeGenerator, AppShellConfigurator {

    private CommandLine.IFactory factory;
    private final CartoConfig config;
    private int exitCode;

    public Application(CommandLine.IFactory factory, CartoConfig config) {
        this.factory = factory;
        this.config = config;
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
