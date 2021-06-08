import Mousetrap from "mousetrap";
export class Shortcuts {
  constructor() {}

  public register(key: string | string[], callback: Function) {
    Mousetrap.bind(key, () => callback());
  }

  public unregister(key: string | string[]) {
    Mousetrap.unbind(key);
  }

  public unregisterAll() {
    Mousetrap.reset();
  }
}

const singletonShortcut = new Shortcuts();

export default singletonShortcut;
