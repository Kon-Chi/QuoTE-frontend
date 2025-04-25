# How to build and run

1. First, compile Scala code.

For dev mode:
```bash
sbt fastLinkJS
```

For production:
```bash
sbt fullLinkJS
```

2. Then compile JS and static files together. It will output everything to `dist` directory.
```bash
npm run build
```

3. Serve `dist` in your preffered way. For development you can use:
```bash
npm run dev
```
