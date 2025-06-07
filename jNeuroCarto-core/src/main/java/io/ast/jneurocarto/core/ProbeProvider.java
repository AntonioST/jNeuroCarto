package io.ast.jneurocarto.core;

/// [ProbeDescription] provider, a java [service][java.util.ServiceLoader].
///
/// ### service
///
/// If you have a [ProbeDescription] implement for custom probe.
/// (The following example are ignoring the package.)
///
/// ```java
/// class MyProbeDescription implements ProbeDescription {
///}
///```
///
/// You have to create a corresponding [ProbeProvider], likes
///
/// ```java
/// class MyProbeProvider implements ProbeProvider {
///     public String name(){ return "myprobe"; }
///     public MyProbeDescription getProbeDescription(){ return new MyProbeDescription(); }
///}
///```
///
/// #### module path
///
/// If your library will be put in the module path (`-p`), then in `module-info.java`
///
/// ```java
/// module my_probe_module {
///     provides ProbeProvider with MyProbeProvider;
///}
///```
///
/// #### class path
///
/// If your library will be put in the class path (`-cp`), then in `module-info.java`.
/// You have to add file `io.ast.jneurocarto.core.ProbeProvider` with the content:
///
/// ```text
/// MyProbeProvider
///```
///
/// and put under folder `META-INFO/services` (`src/main/resources/META-INF/services` for maven project).
///
/// ### plugins
///
/// The package of [ProbeProvider] implementation will be treated as a plugin search root.
/// The detail are described by other service class (for example [ElectrodeSelector]).
/// They may not use [service][java.util.ServiceLoader] approach but using class scanning to
/// dynamic fetch service information.
public interface ProbeProvider {

    /**
     * {@return the family name of the provided probe kind}
     */
    String name();

    /**
     * {@return the short description of the provided probe kind}
     */
    default String description() {
        return "";
    }

    /**
     * Create a new probe description.
     *
     * @return a probe description
     */
    ProbeDescription<?> getProbeDescription();
}
