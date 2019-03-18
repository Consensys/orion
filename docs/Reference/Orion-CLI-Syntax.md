description: Orion command line interface reference
<!--- END of page meta data -->

# Orion Command Line

This reference describes the syntax of the Orion Command Line Interface (CLI) options and subcommands.

```bash
orion [options] [configFile]
```

Runs Orion private transaction manager.

## Options

### generatekeys

```bash tab="Syntax"
-g, --generatekeys <names>
```

```bash tab="Example"
--generatekeys orion
```

Generates public/private key pairs for each name supplied where `<names>` is a comma-seperated list. 

### help

```bash tab="Syntax"
-h, --help
```

Displays help and exits.

### version

```bash tab="Syntax"
  -v, --version
```

Displays version information and exits.

 
## Configuration File 

```bash tab="Syntax"
orion <configFile>
```

```bash tab="Example"
orion orion.conf
```

Specifies the [configuration file](../Configuring-Orion/Configuration-File.md) with which to start Orion. 