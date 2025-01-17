import { AppRegistry, Platform } from 'react-native';
import { Template, TemplateConfig } from '../Template';
import { CarPlay } from '../../CarPlay';
import { NavigationTemplateConfig } from './NavigationTemplate';

export interface AndroidNavigationBaseTemplateConfig extends TemplateConfig {
  /**
   * Your component to render inside Android Auto Map view
   * Example `component: MyComponent`
   */
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  component: React.ComponentType<any>;
  onDidShowPanningInterface?(): void;
  onDidDismissPanningInterface?(): void;
  onMapButtonPressed?(e: { id: string }): void;
  onButtonPressed?(e: { id: string }): void;
  onAlertActionPressed?(e: { secondary?: boolean; primary?: boolean }): void;
  onScroll?(e: { distanceX: number; distanceY: number }): void;
  onScale?(e: { focusX: number; focusY: number; scaleFactor: number }): void;
  onFling?(e: { velocityX: number; velocityY: number }): void;
}

export class AndroidNavigationBaseTemplate<
  T extends AndroidNavigationBaseTemplateConfig,
> extends Template<T> {
  get eventMap() {
    return {
      didShowPanningInterface: 'onDidShowPanningInterface',
      didDismissPanningInterface: 'onDidDismissPanningInterface',
      mapButtonPressed: 'onMapButtonPressed',
      buttonPressed: 'onButtonPressed',
      alertActionPressed: 'onAlertActionPressed',
      scroll: 'onScroll',
      scale: 'onScale',
      fling: 'onFling',
    };
  }

  constructor(public config: T) {
    super(config);

    // Android component registration occurs in the index.js file
    if (config.component && Platform.OS !== 'android') {
      AppRegistry.registerComponent(this.id, () => config.component);
    }

    const callbackFn = Platform.select({
      android: ({ error }: { error?: string } = {}) => {
        error && console.error(error);
      },
    });

    CarPlay.bridge.createTemplate(
      this.id,
      this.parseConfig({ type: this.type, ...config, render: true }),
      callbackFn,
    );
  }

  /**
   * Update MapTemplate configuration
   */
  public updateConfig(config: T) {
    this.config = config;
    CarPlay.bridge.updateMapTemplateConfig(this.id, this.parseConfig(config));
  }

  /**
   * Shows the panning interface over the map.
   *
   * Calling this method while displaying the panning interface has no effect.
   *
   * While showing the panning interface, the system hides all map buttons. The system doesn't provide a button to dismiss the panning interface. Instead, you must provide a map button in the navigation bar that the user taps to dismiss the panning interface.
   * @param animated A Boolean value that determines whether to animate the panning interface.
   */
  public showPanningInterface(animated = false) {
    CarPlay.bridge.showPanningInterface(this.id, animated);
  }

  /**
   * Dismisses the panning interface.
   *
   * When dismissing the panning interface, the system shows the previously hidden map buttons.
   * @param animated A Boolean value that determines whether to animate the dismissal of the panning interface.
   */
  public dismissPanningInterface(animated = false) {
    CarPlay.bridge.dismissPanningInterface(this.id, animated);
  }
}
