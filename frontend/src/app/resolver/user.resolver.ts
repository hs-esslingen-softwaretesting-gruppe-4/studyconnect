import { Injectable } from "@angular/core";
import { UsersServiceWrapper } from "../services/wrapper-services/users.service.wrapper";
import { Resolve } from "@angular/router";

@Injectable({
  providedIn: 'root',
})
/**
 * Resolver to map the currently authenticated user to its ID before loading a route.
 */
export class UserResolver implements Resolve<number | undefined>{
  constructor(private readonly userService: UsersServiceWrapper) {}

  async resolve(): Promise<number | undefined> {
    return this.userService.getUserIDByToken();
  }
}
