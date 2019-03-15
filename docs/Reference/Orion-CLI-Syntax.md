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

Generate public/private key pairs for each of the names supplied where <names> is a comma-seperated list. 

### help

```bash tab="Syntax"
-h, --help
```

Show the help message and exit.

### version

```bash tab="Syntax"
  -v, --version
```

Print version information and exit.

 
## Configuration File 

```bash tab="Syntax"
orion <configFile>
```

```bash tab="Example"
orion orion.conf
```

Specify the [configuration file](../Configuring-Orion/Configuration-File.md) with which to start Orion. 