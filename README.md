# rn-sharing-intent

The rn-share-intent library is a simple and effective solution for enabling users to easily share content from your React Native application to other apps on both Android and iOS platforms. By leveraging native share intents, this library opens the standard share chooser of the operating system, allowing users to share media seamlessly.

## Installation

```sh
npm install rn-share-intent --save
```

or

```sh
yarn add rn-share-intent
```

## Platform-specific Considerations
# Android
Make sure to add the necessary permissions in your AndroidManifest.xml file to allow sharing:

```xlm
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

## Usage

Importing the module

```js
import ShareIntent from 'rn-share-intent';
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
