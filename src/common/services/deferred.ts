export default class Deferred<T> {
    private _resolve: (value: T) => void = () => {};
    private _reject: (value: T) => void = () => {};

    private _promise: Promise<T> = new Promise<T>((resolve, reject) => {
        this._reject = reject;
        this._resolve = resolve;
    })

    public get promise(): Promise<T> {
        return this._promise;
    }

    public resolve(value: T) {
        this._resolve(value);
    }

    public reject(value: T) {
        this._reject(value);
    }
}