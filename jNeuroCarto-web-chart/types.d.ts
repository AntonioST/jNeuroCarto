// This TypeScript modules definition file is generated by vaadin-maven-plugin.
// You can not directly import your different static files into TypeScript,
// This is needed for TypeScript compiler to declare and export as a TypeScript module.
// It is recommended to commit this file to the VCS.
// You might want to change the configurations to fit your preferences
declare module '*.css?inline' {
    import type { CSSResultGroup } from 'lit';
    const content: CSSResultGroup;
    export default content;
}

// Allow any CSS Custom Properties
declare module 'csstype' {
    interface Properties {
        [index: `--${string}`]: any;
    }
}
