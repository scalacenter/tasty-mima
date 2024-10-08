package tastymima

import tastyquery.Contexts.*
import tastyquery.Exceptions.*
import tastyquery.Names.*
import tastyquery.Symbols.*
import tastyquery.Types.*

private[tastymima] final class TypeTranslator(oldCtx: Context, newCtx: Context):
  private val translatedBinders = new java.util.IdentityHashMap[TypeBinder, TypeBinder]()

  def translateType(oldType: Type): Type =
    oldType match
      case oldType: NamedType =>
        oldType.prefix match
          case oldPrefix: PackageRef =>
            val translatedPrefix = newCtx.findPackageFromRoot(oldPrefix.fullyQualifiedName).packageRef
            NamedType(translatedPrefix, oldType.name)
          case oldPrefix: Type =>
            val translatedPrefix = translateType(oldPrefix)
            oldType.optSymbol(using oldCtx) match
              case Some(oldSym: ClassTypeParamSymbol) =>
                translateClassTypeParamRef(translatedPrefix, oldSym)
              case _ =>
                NamedType(translatedPrefix, oldType.name)
          case NoPrefix =>
            throw InvalidProgramStructureException(s"Unexpected local ref $oldType")

      case oldType: ThisType =>
        ThisType(translateType(oldType.tref).asInstanceOf[TypeRef])

      case oldType: SuperType =>
        val translatedThistpe = translateType(oldType.thistpe).asInstanceOf[ThisType]
        val translatedExplicitSupertpe = oldType.explicitSupertpe.map(translateType(_))
        SuperType(translatedThistpe, translatedExplicitSupertpe)

      case oldType: ConstantType =>
        ConstantType(oldType.value)

      case oldType: AppliedType =>
        val translatedTycon = translateType(oldType.tycon)
        val translatedArgs = oldType.args.map { arg =>
          arg match
            case arg: Type            => translateType(arg)
            case arg: WildcardTypeArg => WildcardTypeArg(translateTypeBounds(arg.bounds))
        }
        AppliedType(translatedTycon, translatedArgs)

      case oldType: FlexibleType =>
        FlexibleType(translateType(oldType.nonNullableType))

      case oldType: ByNameType =>
        ByNameType(translateType(oldType.resultType))

      case oldType: RepeatedType =>
        RepeatedType(translateType(oldType.elemType))

      case oldType: TypeLambda =>
        TypeLambda(oldType.paramNames)(
          { lt =>
            translatedBinders.put(oldType, lt)
            oldType.paramInfos.map(translateTypeBounds(_))
          },
          lt => translateType(oldType.resultType)
        )

      case oldType: ParamRef =>
        val translatedParamBinders = translatedBinders.get(oldType.binder).nn.asInstanceOf[ParamRefBinder]
        translatedParamBinders.paramRefs(oldType.paramNum)

      case oldType: AnnotatedType =>
        // There is nothing we can do about the annotation here, so we get rid of it
        translateType(oldType.typ)

      case oldType: TypeRefinement =>
        TypeRefinement(translateType(oldType.parent), oldType.refinedName, translateTypeBounds(oldType.refinedBounds))

      case oldType: TermRefinement =>
        TermRefinement(
          translateType(oldType.parent),
          oldType.isStable,
          oldType.refinedName,
          translateTypeOrMethodic(oldType.refinedType)
        )

      case oldType: RecType =>
        RecType({ rt =>
          translatedBinders.put(oldType, rt)
          translateType(oldType.parent)
        })

      case oldType: RecThis =>
        val translatedRecType = translatedBinders.get(oldType.binder).nn.asInstanceOf[RecType]
        translatedRecType.recThis

      case oldType: MatchType =>
        val translatedBound = translateType(oldType.bound)
        val translatedScrutinee = translateType(oldType.scrutinee)
        val translatedCases = oldType.cases.map(translateMatchTypeCase(_))
        MatchType(translatedBound, translatedScrutinee, translatedCases)

      case _: NothingType =>
        defn(using newCtx).NothingType

      case _: AnyKindType =>
        defn(using newCtx).AnyKindType

      case oldType: OrType =>
        OrType(translateType(oldType.first), translateType(oldType.second))

      case oldType: AndType =>
        AndType(translateType(oldType.first), translateType(oldType.second))

      case oldType: SkolemType =>
        throw InvalidProgramStructureException(s"Unexpected skolem type $oldType")

      case oldType: CustomTransientGroundType =>
        throw InvalidProgramStructureException(s"Unexpected custom transient type $oldType")
  end translateType

  def translateTypeOrMethodic(oldType: TypeOrMethodic): TypeOrMethodic =
    oldType match
      case oldType: Type =>
        translateType(oldType)

      case oldType: MethodType =>
        oldType.companion(oldType.paramNames)(
          { lt =>
            translatedBinders.put(oldType, lt)
            oldType.paramInfos.map(translateType(_))
          },
          lt => translateTypeOrMethodic(oldType.resultType)
        )

      case oldType: PolyType =>
        PolyType(oldType.paramNames)(
          { lt =>
            translatedBinders.put(oldType, lt)
            oldType.paramInfos.map(translateTypeBounds(_))
          },
          lt => translateTypeOrMethodic(oldType.resultType)
        )
  end translateTypeOrMethodic

  def translateTypeBounds(oldBounds: TypeBounds): TypeBounds = oldBounds match
    case AbstractTypeBounds(low, high) => AbstractTypeBounds(translateType(low), translateType(high))
    case TypeAlias(alias)              => TypeAlias(translateType(alias))
  end translateTypeBounds

  private def translateClassTypeParamRef(translatedPrefix: Type, oldSym: ClassTypeParamSymbol): TypeRef =
    // We know that class type param counts match because of a check in `analyzeClass`
    val typeParamIndex = oldSym.owner.typeParams.indexOf(oldSym)
    val translatedSym = translatedPrefix match
      case translatedPrefix: ThisType =>
        translatedPrefix.cls(using newCtx).typeParams(typeParamIndex)
      case _ =>
        throw NotImplementedError(s"cannot translate class type param ref with non-this prefix $translatedPrefix")
    TypeRef(translatedPrefix, translatedSym)
  end translateClassTypeParamRef

  private def translateMatchTypeCase(oldCase: MatchTypeCase): MatchTypeCase =
    MatchTypeCase(oldCase.paramNames)(
      { tmc =>
        translatedBinders.put(oldCase, tmc)
        oldCase.paramTypeBounds.map(translateTypeBounds(_))
      },
      tmc => translateType(oldCase.pattern),
      tmc => translateType(oldCase.result)
    )
  end translateMatchTypeCase
end TypeTranslator
