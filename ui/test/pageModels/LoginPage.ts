
import BasePage from './BasePage'
export default class LoginPage extends BasePage {
    
    public async Validate(): Promise<boolean> {
        const currentUrl = await this.client.getUrl();
        return currentUrl.indexOf("/login") > 0
    }

    public async Login(username:string, password:string){
        await (await this.client.$("[name='username']")).setValue(
            username
          );
          await (await this.client.$("[name='password']")).setValue(
            password
          );
          await (await this.client.$(".btn-custom-auth")).click();
          await this.client.pause(4000);
    }
}