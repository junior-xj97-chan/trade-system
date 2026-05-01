declare module 'json-bigint' {
  interface JSONBigOptions {
    storeAsString?: boolean
    alwaysParseAsBig?: boolean
    strict?: boolean
    constructor?: any
  }
  interface JSONBig {
    parse(text: string, reviver?: (this: any, key: string, value: any) => any): any
    stringify(value: any, replacer?: (this: any, key: string, value: any) => any, space?: string | number): string
  }
  const JSONBigString: JSONBig
  export default JSONBigString
}
