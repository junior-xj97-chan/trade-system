declare module 'echarts' {
  export function init(el: HTMLElement, theme?: string, opts?: any): ECharts
  export interface ECharts {
    setOption(option: any, notMerge?: boolean, lazyUpdate?: boolean): void
    resize(opts?: any): void
    dispose(): void
    on(event: string, handler: Function): void
    off(event: string, handler?: Function): void
    [key: string]: any
  }
  const echarts: {
    init: typeof init
    [key: string]: any
  }
  export default echarts
}
